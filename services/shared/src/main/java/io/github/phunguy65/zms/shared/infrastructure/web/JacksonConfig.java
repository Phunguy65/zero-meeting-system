package io.github.phunguy65.zms.shared.infrastructure.web;

import org.openapitools.jackson.nullable.JsonNullableJackson3Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public JsonNullableJackson3Module jsonNullableJackson3Module() {
        return new JsonNullableJackson3Module();
    }
}
