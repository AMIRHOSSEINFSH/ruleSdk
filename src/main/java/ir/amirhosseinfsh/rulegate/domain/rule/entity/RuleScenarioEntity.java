package ir.amirhosseinfsh.rulegate.domain.rule.entity;

import java.util.HashSet;
import java.util.Set;

public class RuleScenarioEntity {

    private String scenario;

    private Set<RuleItemEntity> ruleItems;

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public Set<RuleItemEntity> getRuleItems() {
        if (ruleItems == null) setRuleItems(new HashSet<>());
        return ruleItems;
    }

    public void setRuleItems(Set<RuleItemEntity> ruleItems) {
        this.ruleItems = ruleItems;
    }
}
