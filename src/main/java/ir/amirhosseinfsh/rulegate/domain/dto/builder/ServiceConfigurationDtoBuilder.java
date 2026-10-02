package ir.amirhosseinfsh.rulegate.domain.dto.builder;

import ir.amirhosseinfsh.rulegate.api.dto.ServiceConfigurationBaseDto;
import ir.amirhosseinfsh.rulegate.api.dto.base.BaseFinalBuilder;
import org.springframework.core.io.support.ResourcePatternResolver;

public class ServiceConfigurationDtoBuilder extends BaseFinalBuilder<ServiceConfigurationBaseDto> {

    private boolean autoConfigure;
    private ResourcePatternResolver resolver;
    private String rulesPath;


    private ServiceConfigurationDtoBuilder() {}

    public static ServiceConfigurationDtoBuilder Builder() {
        return new ServiceConfigurationDtoBuilder();
    }

    public ServiceConfigurationDtoBuilder autoConfigure(boolean autoConfigure) {
        this.autoConfigure = autoConfigure;
        return this;
    }

    public ServiceConfigurationDtoBuilder rulesPath(String rulesPath) {
        this.rulesPath = rulesPath;
        return this;
    }

    public ServiceConfigurationDtoBuilder resolver(ResourcePatternResolver resolver) {
        this.resolver = resolver;
        return this;
    }

    @Override
    public ServiceConfigurationBaseDto build() {
        ServiceConfigurationBaseDto serviceConfigurationBaseDto = new ServiceConfigurationBaseDto();
        serviceConfigurationBaseDto.setAutoConfigure(autoConfigure);
        serviceConfigurationBaseDto.setRulesPath(rulesPath);
        serviceConfigurationBaseDto.setResolver(resolver);
        return serviceConfigurationBaseDto;
    }
}
