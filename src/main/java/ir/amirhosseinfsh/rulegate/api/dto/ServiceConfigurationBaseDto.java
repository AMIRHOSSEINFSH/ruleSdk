package ir.amirhosseinfsh.rulegate.api.dto;

import ir.amirhosseinfsh.rulegate.api.dto.base.BaseBuilder;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ServiceConfigurationBaseDto extends BaseBuilder {
    private boolean autoConfigure;
    private ResourcePatternResolver resolver;
    private String rulesPath;

    public boolean isAutoConfigure() {
        return autoConfigure;
    }

    public void setAutoConfigure(boolean autoConfigure) {
        this.autoConfigure = autoConfigure;
    }

    public ResourcePatternResolver getResolver() {
        return resolver;
    }

    public void setResolver(ResourcePatternResolver resolver) {
        this.resolver = resolver;
    }

    public String getRulesPath() {
        return rulesPath;
    }

    public void setRulesPath(String rulesPath) {
        this.rulesPath = rulesPath;
    }

    @Override
    public void validate() {

    }
}
