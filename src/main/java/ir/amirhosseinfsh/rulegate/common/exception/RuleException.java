package ir.amirhosseinfsh.rulegate.common.exception;

import ir.amirhosseinfsh.rulegate.common.validation.DroolsValidationCode;
import ir.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp;
import ir.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;

public final class RuleException extends DroolsException {
    public RuleException(String message, RuleValidationCode code) {
        super(message, code);
    }
    //todo tmp
    public RuleException(String message, DroolsValidationCodeTmp code) {
        super(message, code);
    }
    public RuleException(String message, DroolsValidationCodeTmp code, Object parameter) {
        super(message, code);
    }

    public RuleException(String message, RuleValidationCode code, Object parameter) {
        super(message, code, parameter);
    }

}
