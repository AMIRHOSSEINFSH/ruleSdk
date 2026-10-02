package ir.amirhosseinfsh.rulegate.domain.dto;

import ir.amirhosseinfsh.rulegate.api.dto.CreateRuleSetBaseDto;

import ir.amirhosseinfsh.rulegate.common.exception.ScenarioException;
import ir.amirhosseinfsh.rulegate.common.validation.ScenarioValidationCode;
import org.kie.api.runtime.KieContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LocalRuleSetDto extends CreateRuleSetBaseDto {
    private static final Logger log = LoggerFactory.getLogger(LocalRuleSetDto.class);
    private KieContainer kieContainer;

    @Override
    public void validate() {
        if (getKieContainer() == null) {
            log.error("kieContainer is required");
            throw new ScenarioException("kieContainer is required", ScenarioValidationCode.KIECONTAINER_REQUIRED);
        }

        if (getRulescenarioEntity() == null) {
            log.error("rulescenarioEntity is required");
            throw new ScenarioException("rulescenarioEntity is required", ScenarioValidationCode.SOMETHING_WENT_WRONG);
        }

        if (getRulescenarioEntity().getScenario() == null || getRulescenarioEntity().getScenario().isEmpty()) {
            log.error("rulescenarioEntity scenario is required");
            throw new ScenarioException("rulescenarioEntity is required", ScenarioValidationCode.scenario_MUST_EXISTS);
        }

        if (getRulescenarioEntity().getRuleItems() == null || getRulescenarioEntity().getRuleItems().isEmpty()) {
            log.error("ruleItems is required");
            throw new ScenarioException("ruleItems is required", ScenarioValidationCode.RULE_ITEMS_REQUIRED);
        }

    }

    public KieContainer getKieContainer() {
        return kieContainer;
    }

    public void setKieContainer(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }
}
