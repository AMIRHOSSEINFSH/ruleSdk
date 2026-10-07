package io.github.amirhosseinfsh.rulegate.domain.dto.builder;

import io.github.amirhosseinfsh.rulegate.api.dto.ExecuteScenarioBaseDto;
import io.github.amirhosseinfsh.rulegate.api.dto.base.BaseFinalBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExecuteScenarioDtoBuilder extends BaseFinalBuilder<ExecuteScenarioBaseDto> {

    private String scenarioName;
    private List<Object> facts;
    private Map<String, ?> globals;
    private String agendaGroupName;

    private ExecuteScenarioDtoBuilder() {

    }

    public static ExecuteScenarioDtoBuilder Builder() {
        return new ExecuteScenarioDtoBuilder();
    }

    public ExecuteScenarioDtoBuilder withAgendaGroupName(String agendaGroupName) {
        this.agendaGroupName = agendaGroupName;
        return this;
    }
    public ExecuteScenarioDtoBuilder withScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
        return this;
    }
    public ExecuteScenarioDtoBuilder addFact(Object fact) {
        if (this.facts == null) this.facts = new ArrayList<>();
        this.facts.add(fact);
        return this;
    }
    public ExecuteScenarioDtoBuilder withFacts(Object... facts) {
        this.facts = Arrays.asList(facts);
        return this;
    }
    public ExecuteScenarioDtoBuilder withGlobals(Map<String, ?> globals) {
        this.globals = globals;
        return this;
    }

    @Override
    public ExecuteScenarioBaseDto build() {
        ExecuteScenarioBaseDto executeRuleBaseDto = new ExecuteScenarioBaseDto();
        executeRuleBaseDto.setScenarioName(scenarioName);
        executeRuleBaseDto.setFacts(facts);
        executeRuleBaseDto.setGlobals(globals);
        executeRuleBaseDto.setAgendaGroupName(agendaGroupName);
        return executeRuleBaseDto;
    }
}
