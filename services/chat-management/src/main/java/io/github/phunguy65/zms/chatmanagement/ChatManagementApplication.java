package io.github.phunguy65.zms.chatmanagement;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
        scanBasePackages = {
            "io.github.phunguy65.zms.chatmanagement",
            "io.github.phunguy65.zms.shared"
        })
@OpenAPIDefinition(info = @Info(title = "ChatManagement", version = "1.0.0"))
@EnableConfigurationProperties
public class ChatManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatManagementApplication.class, args);
    }
}
