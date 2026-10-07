package io.github.amirhosseinfsh.rulegate.api.dto;

import io.github.amirhosseinfsh.rulegate.api.dto.base.BaseBuilder;
import io.github.amirhosseinfsh.rulegate.common.exception.RuleException;
import io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteRuleBaseDto extends DeleteScenarioBaseDto {
    private String ruleName;

    @Override
    public void validate() {
        super.validate();
        if (ruleName == null || ruleName.isEmpty()) {
            log.error("ruleName is null");
            throw new RuleException("rule name must not be null", RuleValidationCode.RULE_NOT_FOUND);
        }
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
}
