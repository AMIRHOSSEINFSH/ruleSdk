package io.github.amirhosseinfsh.rulegate.execution;

import io.github.amirhosseinfsh.rulegate.config.RuleSdkAutoConfiguration;
import io.github.amirhosseinfsh.rulegate.config.RulesExecutionProperties;
import io.github.amirhosseinfsh.rulegate.common.exception.RuleException;
import io.github.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import io.github.amirhosseinfsh.rulegate.common.validation.RuleValidationCode;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalDroolsRepositoryImpl;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleSdkAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RuleSdkAutoConfiguration.class));

    @Test
    void selectsLocalModeFromProperties() {
        contextRunner.withPropertyValues(
                "rules.execution.mode=local",
                "rules.execution.local.auto-load=false",
                "rules.execution.local.strict-validation-metadata=true")
                .run(context -> {
                    LocalRuleExecutor executor = assertInstanceOf(
                            LocalRuleExecutor.class, context.getBean(RuleExecutor.class));
                    assertTrue(context.getBean(RulesExecutionProperties.class)
                            .getLocal().isStrictValidationMetadata());
                    LocalRuleEngineService service = (LocalRuleEngineService)
                            ReflectionTestUtils.getField(executor, "service");
                    assertNotNull(service);
                    assertTrue(service.isStrictValidationMetadata());
                });
    }

    @Test
    void loadsClasspathRulesInConfiguredLocalMode() {
        contextRunner.withPropertyValues("rules.execution.mode=local")
                .run(context -> {
                    RuleExecutionResult result = context.getBean(RuleExecutor.class).execute(
                            RuleExecutionRequest.local("configured", List.of("ready"), Map.of(), null));
                    assertEquals(List.of(new RuleValidation(
                            "configured check", "Loaded from classpath", "ERROR")),
                            result.validations());
                });
    }

    @Test
    void strictModeRejectsIncompleteClasspathRulesAtStartup() {
        contextRunner.withPropertyValues(
                "rules.execution.mode=local",
                "rules.execution.local.rule-dir=strict-rules",
                "rules.execution.local.strict-validation-metadata=true")
                .run(context -> {
                    Throwable failure = rootCause(context.getStartupFailure());
                    RuleException error = assertInstanceOf(RuleException.class, failure);
                    assertEquals(RuleValidationCode.RULE_VALIDATION_METADATA_MISSING,
                            error.getCode());
                });
    }

    @Test
    void strictModeRejectsAnExistingPermissiveService() {
        contextRunner.withBean(LocalRuleEngineService.class, () -> new LocalRuleEngineService(
                        new LocalDroolsRepositoryImpl(new DroolsHelper()), false))
                .withPropertyValues(
                        "rules.execution.mode=local",
                        "rules.execution.local.strict-validation-metadata=true")
                .run(context -> {
                    Throwable failure = rootCause(context.getStartupFailure());
                    IllegalStateException error = assertInstanceOf(IllegalStateException.class, failure);
                    assertTrue(error.getMessage().contains("constructed without it"));
                });
    }

    @Test
    void selectsRemoteModeFromProperties() {
        contextRunner.withPropertyValues(
                "rules.execution.mode=remote",
                "rules.execution.remote.base-url=http://localhost:8080")
                .run(context -> assertInstanceOf(
                        RemoteRuleExecutor.class, context.getBean(RuleExecutor.class)));
    }

    @Test
    void doesNotSelectModeWhenPropertyIsAbsent() {
        contextRunner.run(context -> assertTrue(context.getBeansOfType(RuleExecutor.class).isEmpty()));
    }

    private static Throwable rootCause(Throwable failure) {
        assertNotNull(failure);
        while (failure.getCause() != null) {
            failure = failure.getCause();
        }
        return failure;
    }
}
