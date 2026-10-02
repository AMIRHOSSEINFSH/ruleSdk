package ir.amirhosseinfsh.rulegate.api.dto;

import ir.amirhosseinfsh.rulegate.api.dto.base.BaseBuilder;
import ir.amirhosseinfsh.rulegate.common.exception.ScenarioException;
import ir.amirhosseinfsh.rulegate.common.validation.ScenarioValidationCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteScenarioBaseDto extends BaseBuilder {

    protected static final Logger log = LoggerFactory.getLogger(DeleteScenarioBaseDto.class);
    private String scenarioName;

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    @Override
    public void validate() {
        if (scenarioName == null || scenarioName.isEmpty()) {
            log.error("scenario name is empty");
            throw new ScenarioException("scenario name is empty", ScenarioValidationCode.scenario_MUST_EXISTS);
        }
    }
}
