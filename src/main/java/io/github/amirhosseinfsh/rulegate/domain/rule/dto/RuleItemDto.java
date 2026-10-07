package io.github.amirhosseinfsh.rulegate.domain.rule.dto;

public class RuleItemDto {

    private String ruleContent;

    public RuleItemDto(String ruleContent) {
        this.ruleContent = ruleContent;
    }

    public RuleItemDto() {
    }

    public String getRuleContent() {
        return ruleContent;
    }

    public void setRuleContent(String ruleContent) {
        this.ruleContent = ruleContent;
    }
}
