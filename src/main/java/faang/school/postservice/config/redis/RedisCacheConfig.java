package faang.school.postservice.config.redis;

import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RedisCacheConfig {

    private final JedisConnectionFactory jedisConnectionFactory;
    private final RedisCacheProperties prop;

    @Bean
    public RedisCacheManager cacheManager() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .entryTtl(Duration.ofMinutes(prop.getGlobalMinutesTtl()));
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(prop.getFeedsCacheName(), defaultConfig.entryTtl(Duration.ofHours(prop.getFeedsHoursTtl())));
        cacheConfigurations.put(prop.getPostsCacheName(), defaultConfig.entryTtl(Duration.ofHours(prop.getPostsHoursTtl())));
//        cacheConfigurations.put(prop.getUsersCacheName(), defaultConfig.entryTtl(Duration.ofHours(prop.getUsersHoursTtl())));

        return RedisCacheManager.builder(jedisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
