package ir.amirhosseinfsh.rulegate.api.dto;

import ir.amirhosseinfsh.rulegate.api.dto.base.BaseBuilder;
import ir.amirhosseinfsh.rulegate.domain.rule.entity.RuleScenarioEntity;

public abstract class CreateRuleSetBaseDto extends BaseBuilder {
    private RuleScenarioEntity rulescenarioEntity;

    public RuleScenarioEntity getRulescenarioEntity() {
        return rulescenarioEntity;
    }

    public void setRuleScenarioEntity(RuleScenarioEntity rulescenarioEntity) {
        this.rulescenarioEntity = rulescenarioEntity;
    }
}
