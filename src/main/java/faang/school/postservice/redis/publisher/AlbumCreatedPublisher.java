package faang.school.postservice.redis.publisher;

import faang.school.postservice.event.AlbumCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumCreatedPublisher {

    @Value("${spring.data.redis.channel.album}")
    private String albumCreatedTopic;

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(AlbumCreatedEvent event) {
        redisTemplate.convertAndSend(albumCreatedTopic, event);
    }
}