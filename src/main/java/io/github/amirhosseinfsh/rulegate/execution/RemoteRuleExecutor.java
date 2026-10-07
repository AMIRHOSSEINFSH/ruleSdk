package io.github.amirhosseinfsh.rulegate.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import io.github.amirhosseinfsh.rulegate.domain.fact.FactDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Sends serialized facts to ruleGate /api/drools/execute. */
public final class RemoteRuleExecutor implements RuleExecutor {
    private final URI endpoint;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final DroolsHelper helper;
    private final Duration requestTimeout;

    public RemoteRuleExecutor(String baseUrl, Duration connectTimeout,
                              Duration requestTimeout) {
        this(baseUrl, HttpClient.newBuilder().connectTimeout(connectTimeout).build(),
                new ObjectMapper().registerModule(new JavaTimeModule()),
                new DroolsHelper(), requestTimeout);
    }

    public RemoteRuleExecutor(String baseUrl, HttpClient client, ObjectMapper mapper,
                              DroolsHelper helper, Duration requestTimeout) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("remote baseUrl is required");
        }
        String normalized = baseUrl.replaceAll("/+$", "");
        this.endpoint = URI.create(normalized + "/api/drools/execute");
        if (!"http".equalsIgnoreCase(endpoint.getScheme())
                && !"https".equalsIgnoreCase(endpoint.getScheme())) {
            throw new IllegalArgumentException("remote baseUrl must use http or https");
        }
        this.client = Objects.requireNonNull(client, "client");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.helper = Objects.requireNonNull(helper, "helper");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
    }

    @Override
    public RuleExecutionResult execute(RuleExecutionRequest request) {
        return execute(request, null);
    }

    @Override
    public RuleExecutionResult execute(RuleExecutionRequest request, String bearerToken) {
        Objects.requireNonNull(request, "request");
        if (request.scenarioId() == null || request.scenarioId() <= 0) {
            throw new IllegalArgumentException("scenarioId is required on every remote execution");
        }
        if (!request.globals().isEmpty() || request.agendaGroupName() != null) {
            throw new IllegalArgumentException(
                    "ruleGate /execute does not accept globals or agendaGroupName");
        }
        List<FactDto> facts = new ArrayList<>();
        for (Object fact : request.facts()) {
            facts.add(fact instanceof FactDto dto ? dto : helper.toFactDto(fact));
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scenarioId", request.scenarioId());
        payload.put("facts", facts);
        try {
            String json = mapper.writeValueAsString(payload);
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            if (bearerToken != null && !bearerToken.isBlank()) {
                builder.header("Authorization", "Bearer " + bearerToken);
            }
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuleExecutionException("ruleGate returned HTTP "
                        + response.statusCode() + ": " + response.body());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.path("content");
            if (!content.isObject()) {
                throw new RuleExecutionException("ruleGate response has no content object");
            }
            List<RuleValidation> validations = new ArrayList<>();
            for (JsonNode item : content.path("ruleValidations")) {
                validations.add(new RuleValidation(
                        textOrNull(item, "ruleName"),
                        textOrNull(item, "message"),
                        textOrNull(item, "type")));
            }
            return new RuleExecutionResult(
                    content.path("scenario").asText(request.scenarioId().toString()),
                    validations);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuleExecutionException("ruleGate request interrupted", e);
        } catch (IOException e) {
            throw new RuleExecutionException("ruleGate request failed", e);
        }
    }

    private static String textOrNull(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
