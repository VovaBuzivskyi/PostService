package faang.school.postservice.publisher.redis.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBanPublisher{

    @Value("${spring.data.redis.channel.user_ban}")
    private String userBanTopic;

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Long message) {
        redisTemplate.convertAndSend(userBanTopic, message);
    }
}