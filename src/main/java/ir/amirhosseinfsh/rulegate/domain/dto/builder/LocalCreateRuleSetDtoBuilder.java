package ir.amirhosseinfsh.rulegate.domain.dto.builder;

import ir.amirhosseinfsh.rulegate.domain.dto.LocalRuleSetDto;
import ir.amirhosseinfsh.rulegate.domain.rule.entity.RuleScenarioEntity;

import org.kie.api.runtime.KieContainer;

public class LocalCreateRuleSetDtoBuilder {
    private KieContainer kieContainer;
    private RuleScenarioEntity rulescenarioEntity;

    private LocalCreateRuleSetDtoBuilder() {}

    public static LocalCreateRuleSetDtoBuilder Builder() {
        return new LocalCreateRuleSetDtoBuilder();
    }

    public LocalCreateRuleSetDtoBuilderStep1 withRuleScenarioEntity(RuleScenarioEntity rulescenarioEntity) {
        this.rulescenarioEntity = rulescenarioEntity;
        return new LocalCreateRuleSetDtoBuilderStep1(this);
    }

    public static class LocalCreateRuleSetDtoBuilderStep1 {
        private final LocalCreateRuleSetDtoBuilder builder;
        public LocalCreateRuleSetDtoBuilderStep1(LocalCreateRuleSetDtoBuilder builder) {
            this.builder = builder;
        }
        public LocalCreateRuleSetDtoBuilderStep2 withKieContainer(KieContainer kieContainer) {
            this.builder.kieContainer = kieContainer;
            return new LocalCreateRuleSetDtoBuilderStep2(builder);
        }
    }
    public static class LocalCreateRuleSetDtoBuilderStep2 {
        private final LocalCreateRuleSetDtoBuilder builder;
        public LocalCreateRuleSetDtoBuilderStep2(LocalCreateRuleSetDtoBuilder builder) {
            this.builder = builder;
        }
        public LocalRuleSetDto build() {
            LocalRuleSetDto localRuleSetDto = new LocalRuleSetDto();
            localRuleSetDto.setRuleScenarioEntity(builder.rulescenarioEntity);
            localRuleSetDto.setKieContainer(builder.kieContainer);
            return localRuleSetDto;
        }
    }

}
