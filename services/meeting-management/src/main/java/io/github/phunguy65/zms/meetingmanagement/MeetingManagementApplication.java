package io.github.phunguy65.zms.meetingmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeetingManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingManagementApplication.class, args);
    }
}
