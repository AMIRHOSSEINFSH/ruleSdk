package io.github.amirhosseinfsh.rulegate.api.dto;

import io.github.amirhosseinfsh.rulegate.api.dto.base.BaseBuilder;
import io.github.amirhosseinfsh.rulegate.domain.rule.entity.RuleScenarioEntity;

public abstract class CreateRuleSetBaseDto extends BaseBuilder {
    private RuleScenarioEntity rulescenarioEntity;

    public RuleScenarioEntity getRulescenarioEntity() {
        return rulescenarioEntity;
    }

    public void setRuleScenarioEntity(RuleScenarioEntity rulescenarioEntity) {
        this.rulescenarioEntity = rulescenarioEntity;
    }
}
