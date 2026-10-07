package io.github.amirhosseinfsh.rulegate.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.github.amirhosseinfsh.rulegate.domain.fact.FactDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RemoteRuleExecutorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    private record Applicant(int age) { }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsScenarioIdFromEachCallAndReadsFindings() throws Exception {
        List<JsonNode> requests = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/drools/execute", exchange -> {
            try {
                requests.add(mapper.readTree(exchange.getRequestBody()));
                byte[] response = ("""
                        {"content":{"scenario":"remote","ruleValidations":[
                          {"ruleName":"age-check","message":"Age must be positive","type":"ERROR"},
                          {"ruleName":"no-metadata"},
                          {"ruleName":"message-only","message":"Notice"},
                          {"ruleName":"type-only","type":"WARNING"}
                        ]},"validations":[]}
                        """).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
        RemoteRuleExecutor executor = new RemoteRuleExecutor(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1), Duration.ofSeconds(2));

        FactDto fact = new FactDto();
        fact.setName("Applicant");
        fact.setType("Applicant");
        fact.setValue(List.of());
        RuleExecutionResult first = executor.execute(RuleExecutionRequest.remote(186L, List.of(new Applicant(21))));
        executor.execute(RuleExecutionRequest.remote(187L, List.of(fact)));

        assertEquals(186L, requests.get(0).path("scenarioId").asLong());
        assertEquals(187L, requests.get(1).path("scenarioId").asLong());
        JsonNode serializedApplicant = requests.get(0).path("facts").get(0);
        assertEquals("Applicant", serializedApplicant.path("type").asText());
        assertEquals("age", serializedApplicant.path("value").get(0).path("name").asText());
        assertEquals(21, serializedApplicant.path("value").get(0).path("value").asInt());
        assertEquals(List.of(
                new RuleValidation("age-check", "Age must be positive", "ERROR"),
                new RuleValidation("no-metadata", null, null),
                new RuleValidation("message-only", "Notice", null),
                new RuleValidation("type-only", null, "WARNING")), first.validations());
    }

    @Test
    void sendsBearerTokenFromEachCallWithoutRetainingIt() throws Exception {
        List<String> authorizationHeaders = new ArrayList<>();
        List<String> requestBodies = new ArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/drools/execute", exchange -> {
            try {
                authorizationHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
                requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = "{\"content\":{\"scenario\":\"186\",\"ruleValidations\":[]}}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
        RuleExecutor executor = new RemoteRuleExecutor(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1), Duration.ofSeconds(2));
        RuleExecutionRequest request = RuleExecutionRequest.remote(186L, List.of(new Applicant(21)));

        executor.execute(request, "first-token");
        executor.execute(request, "second-token");
        executor.execute(request);

        assertEquals(java.util.Arrays.asList("Bearer first-token", "Bearer second-token", null),
                authorizationHeaders);
        assertTrue(requestBodies.stream().noneMatch(body -> body.contains("first-token")
                || body.contains("second-token")));
    }

    @Test
    void rejectsMissingIdAndUnsupportedRemoteOptions() {
        RemoteRuleExecutor executor = new RemoteRuleExecutor(
                "http://127.0.0.1:1", Duration.ofSeconds(1), Duration.ofSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(
                new RuleExecutionRequest(null, null, List.of("x"), null, null)));
        assertThrows(IllegalArgumentException.class, () -> executor.execute(
                new RuleExecutionRequest(null, 1L, List.of("x"),
                        java.util.Map.of("validations", new ArrayList<>()), null)));
    }
}
