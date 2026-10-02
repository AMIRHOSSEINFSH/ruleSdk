package ir.amirhosseinfsh.rulegate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Settings for the SDK execution adapter; scenario selection remains per request. */
@ConfigurationProperties(prefix = "rules.execution")
public class RulesExecutionProperties {
    public enum Mode { LOCAL, REMOTE }

    private Mode mode;
    private final Local local = new Local();
    private final Remote remote = new Remote();

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public Local getLocal() { return local; }
    public Remote getRemote() { return remote; }

    public static class Local {
        private String ruleDir = "rules";
        private boolean autoLoad = true;

        public String getRuleDir() { return ruleDir; }
        public void setRuleDir(String ruleDir) { this.ruleDir = ruleDir; }
        public boolean isAutoLoad() { return autoLoad; }
        public void setAutoLoad(boolean autoLoad) { this.autoLoad = autoLoad; }
    }

    public static class Remote {
        private String baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration requestTimeout = Duration.ofSeconds(10);

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getRequestTimeout() { return requestTimeout; }
        public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    }
}
