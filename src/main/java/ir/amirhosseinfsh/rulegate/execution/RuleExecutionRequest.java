package ir.amirhosseinfsh.rulegate.execution;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A scenario is selected on each execution. Local execution uses scenarioName;
 * remote execution uses the ruleGate scenarioId.
 */
public record RuleExecutionRequest(
        String scenarioName,
        Long scenarioId,
        List<?> facts,
        Map<String, ?> globals,
        String agendaGroupName) {

    public RuleExecutionRequest {
        Objects.requireNonNull(facts, "facts");
        if (facts.isEmpty()) {
            throw new IllegalArgumentException("facts must not be empty");
        }
        facts = List.copyOf(facts);
        globals = globals == null ? Map.of() : Map.copyOf(globals);
    }

    public static RuleExecutionRequest local(String scenarioName, List<?> facts,
                                             Map<String, ?> globals, String agendaGroupName) {
        return new RuleExecutionRequest(scenarioName, null, facts, globals, agendaGroupName);
    }

    public static RuleExecutionRequest remote(long scenarioId, List<?> facts) {
        return new RuleExecutionRequest(null, scenarioId, facts, Map.of(), null);
    }
}
