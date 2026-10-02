package ir.amirhosseinfsh.rulegate.common.exception;

import ir.amirhosseinfsh.rulegate.common.validation.DroolsValidationCode;

public sealed class DroolsException extends RuntimeException permits RuleException, ScenarioException {
    private final DroolsValidationCode code;
    private Object parameter;

    public DroolsException(String message, DroolsValidationCode code) {
        super(message);
        this.code = code;
        this.parameter = null;
    }

    public DroolsException(String message, DroolsValidationCode code, Object parameter) {
        this(message, code);
        this.parameter = parameter;
    }

    public Object getParameter() {
        return parameter;
    }

    public DroolsValidationCode getCode() {
        return code;
    }
}
