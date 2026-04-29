package io.github.phunguy65.zms.chatmanagement.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual Flyway configuration for MongoDB.
 *
 * <p>Spring Boot's Flyway auto-configuration expects a JDBC migration datasource, so chat-management
 * keeps Flyway wiring manual and allows tests to disable it via {@code spring.flyway.enabled=false}.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayMongoConfig {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Bean
    public Flyway flyway() {
        return Flyway.configure()
                .dataSource(mongoUri, null, null)
                .locations("classpath:db/migration")
                .sqlMigrationSuffixes(".js")
                .load();
    }

    @Bean
    public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }
}
