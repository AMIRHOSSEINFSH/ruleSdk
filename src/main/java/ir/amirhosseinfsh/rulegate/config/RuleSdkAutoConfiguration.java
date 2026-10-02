package ir.amirhosseinfsh.rulegate.config;

import ir.amirhosseinfsh.rulegate.api.context.RuleEngineServiceContext;
import ir.amirhosseinfsh.rulegate.common.helper.DroolsHelper;
import ir.amirhosseinfsh.rulegate.domain.impl.LocalDroolsRepositoryImpl;
import ir.amirhosseinfsh.rulegate.domain.impl.LocalRuleEngineService;
import ir.amirhosseinfsh.rulegate.execution.LocalRuleExecutor;
import ir.amirhosseinfsh.rulegate.execution.RemoteRuleExecutor;
import ir.amirhosseinfsh.rulegate.execution.RuleExecutor;
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
                    new PathMatchingResourcePatternResolver());
        } else if (existing instanceof LocalRuleEngineService local) {
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
