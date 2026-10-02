package ir.amirhosseinfsh.rulegate.common.validation;

public enum DroolsValidationCodeTmp implements DroolsValidationCode {

    RULE_NOT_FOUND,
    SCENARIO_NOT_FOUND,
    PACKAGE_NOT_FOUND,
    MULTIPLE_RULES_FOUND,
    MULTIPLE_SAME_RULES_FOUND,
    GLOBAL_VARIABLE_NOT_FOUND,
    FACT_VARIABLE_NOT_FOUND,
    DROOLS_EXECUTION_ERROR,
    DROOLS_BUILD_ERROR,
    GLOBAL_VARIABLE_DUPLICATED_FOUND,
    DUPLICATE_RULE_FOUND,
    DRL_PATH_NOT_FOUND,
    ;

    @Override
    public String getCode() {
        return this.name();
    }
}
