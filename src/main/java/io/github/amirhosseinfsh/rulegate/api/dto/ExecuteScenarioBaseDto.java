package io.github.amirhosseinfsh.rulegate.api.dto;

import io.github.amirhosseinfsh.rulegate.api.dto.base.BaseBuilder;
import io.github.amirhosseinfsh.rulegate.common.exception.ScenarioException;
import io.github.amirhosseinfsh.rulegate.common.validation.ScenarioValidationCode;


import java.util.List;
import java.util.Map;

public class ExecuteScenarioBaseDto extends BaseBuilder {

    private String scenarioName;
    private List<Object> facts;
    private Map<String, ?> globals;
    private String agendaGroupName;

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public List<Object> getFacts() {
        return facts;
    }

    public void setFacts(List<Object> facts) {
        this.facts = facts;
    }

    public Map<String,?> getGlobals() {
        return globals;
    }

    public void setGlobals(Map<String,?> globals) {
        this.globals = globals;
    }

    public String getAgendaGroupName() {
        return agendaGroupName;
    }

    public void setAgendaGroupName(String agendaGroupName) {
        this.agendaGroupName = agendaGroupName;
    }

    @Override
    public void validate() {
        if (scenarioName == null) {
            String message  = "scenarioName is required";
            throw new ScenarioException(message, ScenarioValidationCode.EXECUTION_ERROR);
        }
        if (facts == null) {
            String message  = "facts is required";
            throw new ScenarioException(message, ScenarioValidationCode.EXECUTION_ERROR);
        }
        if (globals == null) {
            String message  = "globals is required";
            throw new ScenarioException(message, ScenarioValidationCode.EXECUTION_ERROR);
        }
    }
}
