package io.github.amirhosseinfsh.rulegate.execution;

import io.github.amirhosseinfsh.rulegate.api.dto.ExecuteScenarioBaseDto;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;

import java.util.ArrayList;
import java.util.Objects;

/** Adapter for existing classpath DRL scenarios. */
public final class LocalRuleExecutor implements RuleExecutor {
    private final LocalRuleEngineService service;

    public LocalRuleExecutor(LocalRuleEngineService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @Override
    public RuleExecutionResult execute(RuleExecutionRequest request, String bearerToken) {
        // The token is only used by the remote adapter. Local mode remains callable
        // through the same application code that supplies a token at runtime.
        return execute(request);
    }

    @Override
    public RuleExecutionResult execute(RuleExecutionRequest request) {
        Objects.requireNonNull(request, "request");
        if (request.scenarioName() == null || request.scenarioName().isBlank()) {
            throw new IllegalArgumentException("scenarioName is required for local execution");
        }
        ExecuteScenarioBaseDto dto = new ExecuteScenarioBaseDto();
        dto.setScenarioName(request.scenarioName());
        dto.setFacts(new ArrayList<>(request.facts()));
        dto.setGlobals(request.globals());
        dto.setAgendaGroupName(request.agendaGroupName());
        return service.executeForResult(dto);
    }
}
