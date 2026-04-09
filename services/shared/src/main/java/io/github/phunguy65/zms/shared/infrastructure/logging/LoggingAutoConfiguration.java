package io.github.phunguy65.zms.shared.infrastructure.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Auto-configuration that registers {@link HttpLoggingFilter} and {@link LoggingAspect} beans.
 * Activated automatically via Spring Boot SPI — no explicit {@code @Import} needed in services.
 */
@AutoConfiguration
@EnableAspectJAutoProxy
public class LoggingAutoConfiguration {

    @Bean
    public HttpLoggingFilter httpLoggingFilter() {
        return new HttpLoggingFilter();
    }

    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
