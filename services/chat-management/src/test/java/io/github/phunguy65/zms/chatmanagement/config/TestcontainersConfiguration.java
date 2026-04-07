package io.github.phunguy65.zms.chatmanagement.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer("mongo:8.0");
    }
}
