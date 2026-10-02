package ir.amirhosseinfsh.rulegate.domain.rule.viewDto;

import java.util.Set;

public class RuleViewDto {
    private String scenario;

    private Set<RuleItemViewDto> ruleItems;

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Set<RuleItemViewDto> getRuleItems() {
        return ruleItems;
    }

    public void setRuleItems(Set<RuleItemViewDto> ruleItems) {
        this.ruleItems = ruleItems;
    }
}
