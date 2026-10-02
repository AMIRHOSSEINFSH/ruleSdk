package ir.amirhosseinfsh.rulegate.execution;

import ir.amirhosseinfsh.rulegate.config.RuleSdkAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
                "rules.execution.local.auto-load=false")
                .run(context -> assertInstanceOf(
                        LocalRuleExecutor.class, context.getBean(RuleExecutor.class)));
    }

    @Test
    void loadsClasspathRulesInConfiguredLocalMode() {
        contextRunner.withPropertyValues("rules.execution.mode=local")
                .run(context -> {
                    RuleExecutionResult result = context.getBean(RuleExecutor.class).execute(
                            RuleExecutionRequest.local("configured", List.of("ready"), Map.of(), null));
                    assertEquals("Loaded from classpath", result.validations().getFirst().message());
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
}
