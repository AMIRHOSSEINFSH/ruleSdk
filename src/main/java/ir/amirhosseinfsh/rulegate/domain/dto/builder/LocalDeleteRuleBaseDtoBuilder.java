package ir.amirhosseinfsh.rulegate.domain.dto.builder;

import ir.amirhosseinfsh.rulegate.api.dto.DeleteRuleBaseDto;
import ir.amirhosseinfsh.rulegate.api.dto.base.BaseFinalBuilder;

public class LocalDeleteRuleBaseDtoBuilder {
    private String scenario;
    private String ruleName;

    private LocalDeleteRuleBaseDtoBuilder() {
    }
    public static LocalDeleteRuleBaseDtoBuilder Builder() {
        return new LocalDeleteRuleBaseDtoBuilder();
    }

    public LocalDeleteRuleByScenarioBuilderStep1 withRuleName(String ruleName) {
        this.ruleName = ruleName;
        return new LocalDeleteRuleByScenarioBuilderStep1(this);
    }

    public static class LocalDeleteRuleByRuleNameBuilderStep1 extends BaseFinalBuilder<DeleteRuleBaseDto> {
        private final LocalDeleteRuleBaseDtoBuilder builder;
        public LocalDeleteRuleByRuleNameBuilderStep1(LocalDeleteRuleBaseDtoBuilder builder) {
            this.builder = builder;
        }
        public LocalDeleteRuleByRuleNameBuilderStep1 withRuleName(String ruleName) {
            builder.ruleName = ruleName;
            return this;
        }
        @Override
        public DeleteRuleBaseDto build() {
            DeleteRuleBaseDto deleteRuleBaseDto = new DeleteRuleBaseDto();
            deleteRuleBaseDto.setRuleName(builder.ruleName);
            return deleteRuleBaseDto;
        }

    }

    public static class LocalDeleteRuleByScenarioBuilderStep1 extends BaseFinalBuilder<DeleteRuleBaseDto> {
        private final LocalDeleteRuleBaseDtoBuilder builder;
        public LocalDeleteRuleByScenarioBuilderStep1(LocalDeleteRuleBaseDtoBuilder builder) {
            this.builder = builder;
        }
        public LocalDeleteRuleByScenarioBuilderStep1 withScenarioName(String scenarioName) {
            builder.scenario = scenarioName;
            return this;
        }

        @Override
        public DeleteRuleBaseDto build() {
            DeleteRuleBaseDto deleteRuleBaseDto = new DeleteRuleBaseDto();
            deleteRuleBaseDto.setScenarioName(builder.scenario);
            deleteRuleBaseDto.setRuleName(builder.ruleName);
            return deleteRuleBaseDto;
        }

    }


}
