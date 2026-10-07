package io.github.amirhosseinfsh.rulegate.common.exception;

import io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCode;
import io.github.amirhosseinfsh.rulegate.common.validation.DroolsValidationCodeTmp;
import io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;

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
