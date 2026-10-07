package io.github.amirhosseinfsh.rulegate.domain.dto.builder;

import io.github.amirhosseinfsh.rulegate.api.dto.DeleteScenarioBaseDto;
import io.github.amirhosseinfsh.rulegate.api.dto.base.BaseFinalBuilder;

public class LocalDeleteScenarioBaseDtoBuilder extends BaseFinalBuilder<DeleteScenarioBaseDto> {
    private String scenarioName;

    private LocalDeleteScenarioBaseDtoBuilder() {
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public LocalDeleteScenarioBaseDtoBuilder withScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
        return this;
    }

    @Override
    public DeleteScenarioBaseDto build() {
        DeleteScenarioBaseDto deletescenarioBaseDto = new DeleteScenarioBaseDto();
        deletescenarioBaseDto.setScenarioName(scenarioName);
        return deletescenarioBaseDto;
    }

}
