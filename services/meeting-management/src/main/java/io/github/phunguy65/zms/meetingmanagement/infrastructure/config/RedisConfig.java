package io.github.phunguy65.zms.meetingmanagement.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for join request queue and SSE Pub/Sub.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link StringRedisTemplate} for join request storage (Sorted Set + Hash)</li>
 *   <li>{@link RedisMessageListenerContainer} for SSE event broadcasting across instances</li>
 * </ul>
 *
 * <p>Only activates when {@code spring.data.redis.host} is set.
 */
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.host")
public class RedisConfig {

    /**
     * Template for string-based Redis operations (join request queue).
     *
     * <p>Uses {@link StringRedisSerializer} for both keys and values to ensure
     * compatibility with Redis CLI inspection and cross-language interop.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * Message listener container for Redis Pub/Sub (SSE event broadcasting).
     *
     * <p>Allows multiple backend instances to subscribe to {@code meeting:*:events}
     * channels and fan-out SSE notifications to connected clients.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
