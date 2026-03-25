package io.github.phunguy65.zms.meetingmanagement.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LiveKit connection properties loaded from environment variables or application.properties.
 *
 * <p>Properties are resolved at startup. Changes require service restart (use K8s rolling restart
 * for zero-downtime updates).
 *
 * <p>Default values ensure the service starts safely in local development.
 */
@Component
@ConfigurationProperties(prefix = "app.livekit")
public class LiveKitProperties {

    /**
     * LiveKit server HTTP URL. Default: {@code http://localhost:7880}.
     */
    private String url = "http://localhost:7880";

    /**
     * LiveKit API key.
     */
    private String apiKey;

    /**
     * LiveKit API secret.
     */
    private String apiSecret;

    /**
     * JWT token TTL in seconds. Default: 1800 (30 minutes).
     */
    private long tokenExpirySeconds = 1800;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public long getTokenExpirySeconds() {
        return tokenExpirySeconds;
    }

    public void setTokenExpirySeconds(long tokenExpirySeconds) {
        this.tokenExpirySeconds = tokenExpirySeconds;
    }
}
