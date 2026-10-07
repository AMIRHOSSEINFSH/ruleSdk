package io.github.amirhosseinfsh.rulegate.common.validation;

public enum RuleValidationCode implements DroolsValidationCode{

    RULE_NOT_FOUND,
    PACKAGE_NOT_FOUND,
    MULTIPLE_RULES_FOUND,
    RULE_SYNTAX_ERROR,
    RULE_VALIDATION_METADATA_MISSING,
    RULE_VALIDATION_METADATA_INVALID,
    GLOBAL_VARIABLE_DUPLICATED_FOUND
    ;

    @Override
    public String getCode() {
        return this.name();
    }
}
