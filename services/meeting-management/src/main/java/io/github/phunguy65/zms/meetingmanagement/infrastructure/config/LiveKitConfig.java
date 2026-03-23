package io.github.phunguy65.zms.meetingmanagement.infrastructure.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the LiveKit server SDK client as a Spring bean.
 *
 * <p>Client is created at startup using properties from environment variables or
 * application.properties. Changes require service restart (use K8s rolling restart for
 * zero-downtime updates).
 */
@Configuration
public class LiveKitConfig {

    @Bean
    public RoomServiceClient roomServiceClient(LiveKitProperties props) {
        return RoomServiceClient.createClient(
                props.getUrl(), props.getApiKey(), props.getApiSecret());
    }
}
