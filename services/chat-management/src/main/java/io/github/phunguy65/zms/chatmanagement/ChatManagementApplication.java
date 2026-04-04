package io.github.phunguy65.zms.chatmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
        scanBasePackages = {
            "io.github.phunguy65.zms.chatmanagement",
            "io.github.phunguy65.zms.shared"
        })
@EnableConfigurationProperties
public class ChatManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatManagementApplication.class, args);
    }
}
