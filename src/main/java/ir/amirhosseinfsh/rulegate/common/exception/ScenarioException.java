package ir.amirhosseinfsh.rulegate.common.exception;


import ir.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp;
import ir.amirhosseinfsh.rulegate.common.validation.ScenarioValidationCode;

public final class ScenarioException extends DroolsException {
    public ScenarioException(String message, ScenarioValidationCode code) {
        super(message, code);
    }
    //todo tmp
    public ScenarioException(String message, DroolsValidationCodeTmp code) {
        super(message, code);
    }
    public ScenarioException(String message, DroolsValidationCodeTmp code, Object parameter) {
        super(message, code);
    }

    public ScenarioException(String message, ScenarioValidationCode code, Object parameter) {
        super(message, code, parameter);
    }
}
