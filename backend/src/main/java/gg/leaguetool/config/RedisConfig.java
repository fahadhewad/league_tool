package gg.leaguetool.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared, cross-instance caching backed by Redis, enabled with {@code app.redis.enabled=true}.
 *
 * <p>Uses the same {@link CacheConfig#SPECS cache names and TTLs} as the in-process Caffeine
 * manager, so enabling Redis changes only where entries live, not the cache semantics. This bean
 * replaces the Caffeine {@code cacheManager} (the two are mutually exclusive via the property).
 */
@Configuration
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()
                .prefixCacheNameWith("leaguetool:cache:");

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        for (CacheConfig.CacheSpec spec : CacheConfig.SPECS) {
            perCache.put(spec.name(), defaults.entryTtl(spec.ttl()));
        }

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaults)
                .withInitialCacheConfigurations(perCache)
                .build();
    }
}
