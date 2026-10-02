package ir.amirhosseinfsh.rulegate.execution;

import ir.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import ir.amirhosseinfsh.rulegate.domain.impl.LocalDroolsRepositoryImpl;
import ir.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;
import ir.amirhosseinfsh.rulegate.domain.rule.dto.RuleDto;
import ir.amirhosseinfsh.rulegate.domain.rule.dto.RuleItemDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocalRuleExecutorTest {
    @Test
    void executesLocalDrlAndPreservesGlobalCollector() {
        LocalRuleEngineService service = new LocalRuleEngineService(
                new LocalDroolsRepositoryImpl(new DroolsHelper()), false);
        RuleDto scenario = new RuleDto();
        scenario.setScenario("local-check");
        scenario.getRuleItems().add(new RuleItemDto("""
                package sample
                global java.util.List validations;
                rule "check"
                @MESSAGE("Matched locally")
                @ERROR(false)
                when
                    String(this == "ok")
                then
                    validations.add("hit");
                end
                """));
        scenario.getRuleItems().add(new RuleItemDto("""
                package sample
                rule "calculation only"
                when
                    String(this == "ok")
                then
                    // A fired rule without validation metadata is not a validation error.
                end
                """));
        service.createScenario(scenario);
        LocalRuleExecutor executor = new LocalRuleExecutor(service);
        List<String> collected = new ArrayList<>();

        RuleExecutionResult result = executor.execute(RuleExecutionRequest.local(
                "local-check", List.of("ok"), Map.of("validations", collected), null),
                "ignored-in-local-mode");

        assertEquals(List.of("hit"), collected);
        assertEquals("local-check", result.scenario());
        assertEquals(1, result.validations().size());
        assertEquals("check", result.validations().getFirst().ruleName());
        assertEquals("Matched locally", result.validations().getFirst().message());
        assertEquals("WARNING", result.validations().getFirst().type());
    }

    @Test
    void failedRuleUpdateKeepsPreviousScenario() {
        DroolsHelper helper = new DroolsHelper();
        LocalDroolsRepositoryImpl repository = new LocalDroolsRepositoryImpl(helper);
        LocalRuleEngineService service = new LocalRuleEngineService(repository, false);
        String validRule = """
                package sample
                rule "stable"
                when
                    String(this == "ok")
                then
                end
                """;
        RuleDto scenario = new RuleDto();
        scenario.setScenario("stable-scenario");
        scenario.getRuleItems().add(new RuleItemDto(validRule));
        service.createScenario(scenario);

        assertThrows(ir.amirhosseinfsh.rulegate.common.exception.RuleException.class, () ->
                repository.updateRuleContent("stable", """
                        package sample
                        rule "stable"
                        when
                            String(this == )
                        then
                        end
                        """));

        assertEquals(validRule, repository.getRuleContent("stable"));
        assertEquals("stable-scenario", new LocalRuleExecutor(service).execute(
                RuleExecutionRequest.local("stable-scenario",
                        List.of("ok"), Map.of(), null)).scenario());
    }

    @Test
    void duplicateRuleNamesAreRejected() {
        DroolsHelper helper = new DroolsHelper();
        String rule = """
                package sample
                rule "duplicate"
                when
                then
                end
                """;
        assertThrows(ir.amirhosseinfsh.rulegate.common.exception.RuleException.class,
                () -> helper.validateDuplicateRules(List.of(rule, rule)));
    }
}
