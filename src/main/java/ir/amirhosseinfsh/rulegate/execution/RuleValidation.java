package ir.amirhosseinfsh.rulegate.execution;

/** A rule finding shared by local and remote execution. */
public record RuleValidation(String ruleName, String message, String type) {
}
