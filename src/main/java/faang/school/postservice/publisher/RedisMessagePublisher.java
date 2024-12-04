package faang.school.postservice.publisher;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessagePublisher implements MessagePublisher<String> {

    @Value("${spring.data.redis.channels.user-ban}")
    private String userBan;

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(String message) {
        redisTemplate.convertAndSend(userBan, message);
    }
}