package io.github.amirhosseinfsh.rulegate.execution;

import java.util.List;
import java.util.Objects;

public record RuleExecutionResult(String scenario, List<RuleValidation> validations) {
    public RuleExecutionResult {
        Objects.requireNonNull(scenario, "scenario");
        validations = List.copyOf(Objects.requireNonNull(validations, "validations"));
    }
}
