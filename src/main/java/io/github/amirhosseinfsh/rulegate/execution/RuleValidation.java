package io.github.amirhosseinfsh.rulegate.execution;

/** A rule finding shared by local and remote execution. Missing metadata is represented by null. */
public record RuleValidation(String ruleName, String message, String type) {
}
