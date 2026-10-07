package io.github.amirhosseinfsh.rulegate.execution;

import io.github.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import io.github.amirhosseinfsh.rulegate.common.exception.RuleException;
import io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalDroolsRepositoryImpl;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;
import io.github.amirhosseinfsh.rulegate.domain.rule.dto.RuleDto;
import io.github.amirhosseinfsh.rulegate.domain.rule.dto.RuleItemDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                    // Fired rules without metadata are returned with null fields.
                end
                """));
        scenario.getRuleItems().add(new RuleItemDto("""
                package sample
                rule "message only"
                @MESSAGE("Only a message")
                when
                    String(this == "ok")
                then
                end
                """));
        scenario.getRuleItems().add(new RuleItemDto("""
                package sample
                rule "error only"
                @ERROR(true)
                when
                    String(this == "ok")
                then
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
        assertEquals(Set.of(
                new RuleValidation("check", "Matched locally", "WARNING"),
                new RuleValidation("calculation only", null, null),
                new RuleValidation("message only", "Only a message", null),
                new RuleValidation("error only", null, "ERROR")),
                Set.copyOf(result.validations()));
    }

    @Test
    void strictMetadataRejectsIncompleteRulesBeforeCompilation() {
        LocalRuleEngineService service = new LocalRuleEngineService(
                new LocalDroolsRepositoryImpl(new DroolsHelper()), false, true);
        for (String annotation : List.of("", "@MESSAGE(\"Required\")", "@ERROR(true)")) {
            RuleDto scenario = new RuleDto();
            scenario.setScenario("strict-check");
            scenario.getRuleItems().add(new RuleItemDto("""
                    package sample
                    rule "check"
                    %s
                    when
                        String(this == "ok")
                    then
                    end
                    """.formatted(annotation)));

            RuleException error = assertThrows(RuleException.class,
                    () -> service.createScenario(scenario));
            assertEquals(RuleValidationCode.RULE_VALIDATION_METADATA_MISSING, error.getCode());
            assertFalse(service.isScenarioExists("strict-check"));
        }

        for (String annotations : List.of(
                "@MESSAGE(\"Required\")\n@ERROR(other)",
                "@MESSAGE(\"\")\n@ERROR(true)")) {
            RuleDto scenario = new RuleDto();
            scenario.setScenario("strict-check");
            scenario.getRuleItems().add(new RuleItemDto("""
                    package sample
                    rule "check"
                    %s
                    when
                        String(this == "ok")
                    then
                    end
                    """.formatted(annotations)));

            RuleException error = assertThrows(RuleException.class,
                    () -> service.createScenario(scenario));
            assertEquals(RuleValidationCode.RULE_VALIDATION_METADATA_INVALID, error.getCode());
            assertFalse(service.isScenarioExists("strict-check"));
        }

        RuleDto validScenario = new RuleDto();
        validScenario.setScenario("strict-check");
        validScenario.getRuleItems().add(new RuleItemDto("""
                package sample
                rule "check"
                @MESSAGE("Required")
                @ERROR(true)
                when
                    String(this == "ok")
                then
                end
                """));
        service.createScenario(validScenario);
        assertEquals(List.of(new RuleValidation("check", "Required", "ERROR")),
                new LocalRuleExecutor(service).execute(RuleExecutionRequest.local(
                        "strict-check", List.of("ok"), Map.of(), null)).validations());

        RuleException updateError = assertThrows(RuleException.class, () -> service.updateRule("""
                package sample
                rule "check"
                when
                    String(this == "ok")
                then
                end
                """));
        assertEquals(RuleValidationCode.RULE_VALIDATION_METADATA_MISSING, updateError.getCode());
        assertEquals(List.of(new RuleValidation("check", "Required", "ERROR")),
                new LocalRuleExecutor(service).execute(RuleExecutionRequest.local(
                        "strict-check", List.of("ok"), Map.of(), null)).validations());
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

        assertThrows(io.github.amirhosseinfsh.rulegate.common.exception.RuleException.class, () ->
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
        assertThrows(io.github.amirhosseinfsh.rulegate.common.exception.RuleException.class,
                () -> helper.validateDuplicateRules(List.of(rule, rule)));
    }
}
