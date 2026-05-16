package org.settlehub.iam.core.security.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

@Configuration
public class SpringDocConfig {

    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
            .group("v1")
            .pathsToMatch("/**")
            .displayName("SettleHub API V.0")
            .build();
    }
}
