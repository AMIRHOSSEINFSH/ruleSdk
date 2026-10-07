package io.github.amirhosseinfsh.rulegate.config;

import io.github.amirhosseinfsh.rulegate.api.context.RuleEngineServiceContext;
import io.github.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalDroolsRepositoryImpl;
import io.github.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;
import io.github.amirhosseinfsh.rulegate.execution.LocalRuleExecutor;
import io.github.amirhosseinfsh.rulegate.execution.RemoteRuleExecutor;
import io.github.amirhosseinfsh.rulegate.execution.RuleExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Creates the selected execution adapter only when rules.execution.mode is set.
 * Existing applications without this property retain their current behavior.
 */
@AutoConfiguration
@EnableConfigurationProperties(RulesExecutionProperties.class)
public class RuleSdkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RuleExecutor.class)
    @ConditionalOnProperty(prefix = "rules.execution", name = "mode", havingValue = "local")
    public RuleExecutor localRuleExecutor(RulesExecutionProperties properties,
                                          ObjectProvider<RuleEngineServiceContext> existingServices) {
        RuleEngineServiceContext existing = existingServices.getIfAvailable();
        LocalRuleEngineService service;
        if (existing == null) {
            DroolsHelper helper = new DroolsHelper();
            service = new LocalRuleEngineService(
                    new LocalDroolsRepositoryImpl(helper),
                    properties.getLocal().isAutoLoad(),
                    properties.getLocal().getRuleDir(),
                    new PathMatchingResourcePatternResolver(),
                    properties.getLocal().isStrictValidationMetadata());
        } else if (existing instanceof LocalRuleEngineService local) {
            if (properties.getLocal().isStrictValidationMetadata()
                    && !local.isStrictValidationMetadata()) {
                throw new IllegalStateException("Strict validation metadata is enabled, but the provided "
                        + "LocalRuleEngineService was constructed without it");
            }
            service = local;
        } else {
            throw new IllegalStateException(
                    "Local mode requires LocalRuleEngineService, but another RuleEngineServiceContext exists");
        }
        return new LocalRuleExecutor(service);
    }

    @Bean
    @ConditionalOnMissingBean(RuleExecutor.class)
    @ConditionalOnProperty(prefix = "rules.execution", name = "mode", havingValue = "remote")
    public RuleExecutor remoteRuleExecutor(RulesExecutionProperties properties) {
        var remote = properties.getRemote();
        return new RemoteRuleExecutor(remote.getBaseUrl(), remote.getConnectTimeout(),
                remote.getRequestTimeout());
    }
}
