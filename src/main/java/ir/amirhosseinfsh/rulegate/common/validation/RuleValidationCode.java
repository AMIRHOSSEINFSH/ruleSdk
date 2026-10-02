package ir.amirhosseinfsh.rulegate.common.validation;

public enum RuleValidationCode implements DroolsValidationCode{

    RULE_NOT_FOUND,
    PACKAGE_NOT_FOUND,
    MULTIPLE_RULES_FOUND,
    RULE_SYNTAX_ERROR,
    GLOBAL_VARIABLE_DUPLICATED_FOUND
    ;

    @Override
    public String getCode() {
        return this.name();
    }
}
