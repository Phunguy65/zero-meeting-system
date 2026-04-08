package io.github.phunguy65.zms.chatmanagement.infrastructure.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual Flyway configuration for MongoDB.
 *
 * <p>Spring Boot's default {@code FlywayAutoConfiguration} assumes a JDBC {@code DataSource},
 * which does not apply to MongoDB. This configuration creates a Flyway instance that targets
 * MongoDB via <a href="https://documentation.red-gate.com/flyway/flyway-concepts/migrations/migration-types/native-connectors">
 * Native Connectors</a> instead.
 *
 * <p>Requires {@code org.flywaydb:flyway-database-mongodb} on the classpath and {@code mongosh}
 * on the system PATH at runtime. The container image built by {@code bootBuildImage} includes
 * {@code mongosh} via APT bindings (see {@code bindings/apt/}). For local development, install
 * {@code mongosh} manually.
 *
 * <p>Migration scripts are JavaScript files ({@code .js}) placed in
 * {@code src/main/resources/db/migration/} and follow the standard Flyway naming convention
 * ({@code V{version}__{description}.js}).
 */
@Configuration
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
