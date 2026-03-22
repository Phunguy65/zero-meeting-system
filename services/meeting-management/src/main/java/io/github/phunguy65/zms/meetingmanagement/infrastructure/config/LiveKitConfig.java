package io.github.phunguy65.zms.meetingmanagement.infrastructure.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the LiveKit server SDK client as a Spring bean.
 *
 * <p>Annotated with {@code @RefreshScope} so the client is recreated when LiveKit connection
 * properties change in Consul KV, without requiring a service restart.
 */
@Configuration
public class LiveKitConfig {

    @Bean
    @RefreshScope
    public RoomServiceClient roomServiceClient(LiveKitProperties props) {
        return RoomServiceClient.createClient(
                props.getUrl(), props.getApiKey(), props.getApiSecret());
    }
}
