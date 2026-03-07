package io.github.phunguy65.zms.usermanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {
            "io.github.phunguy65.zms.usermanagement",
            "io.github.phunguy65.zms.shared"
        })
@EnableScheduling
public class UserManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserManagementApplication.class, args);
    }
}
