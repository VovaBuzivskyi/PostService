package faang.school.postservice.publisher.redis.impl;

import faang.school.postservice.publisher.redis.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessagePublisher implements MessagePublisher<String> {

    @Value("${spring.data.redis.channel.user-ban}")
    private String userBan;

    private final RedisTemplate<String, String> redisTemplate;

    public void publish(String message) {
        redisTemplate.convertAndSend(userBan, message);
    }
}