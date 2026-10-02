package ir.amirhosseinfsh.rulegate.execution;

/** Execute one scenario using the application-configured mode. */
public interface RuleExecutor {
    RuleExecutionResult execute(RuleExecutionRequest request);

    /** Supply a bearer token for this call only when the remote mode requires one. */
    default RuleExecutionResult execute(RuleExecutionRequest request, String bearerToken) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            throw new UnsupportedOperationException("This RuleExecutor does not support bearer authentication");
        }
        return execute(request);
    }
}
