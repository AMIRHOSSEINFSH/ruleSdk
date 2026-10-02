package ir.amirhosseinfsh.rulegate.domain.rule.dto;


import java.util.HashSet;
import java.util.Set;

public class RuleDto  {

    private String scenario;

    private Set<RuleItemDto> ruleItems;

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Set<RuleItemDto> getRuleItems() {
        if (ruleItems == null)
            ruleItems = new HashSet<>();
        return ruleItems;
    }

    public void setRuleItems(Set<RuleItemDto> ruleItems) {
        this.ruleItems = ruleItems;
    }

}
