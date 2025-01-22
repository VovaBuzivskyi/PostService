package faang.school.postservice.publisher.redis;

import faang.school.postservice.event.AlbumCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

public interface MessagePublisher<T> {
    void publish(T message);

    @Component
    @RequiredArgsConstructor
    class AlbumCreatedPublisher {

        @Value("${spring.data.redis.channel.album}")
        private String albumCreatedTopic;

        private final RedisTemplate<String, Object> redisTemplate;

        public void publish(AlbumCreatedEvent event) {
            redisTemplate.convertAndSend(albumCreatedTopic, event);
        }
    }

    @Component
    @RequiredArgsConstructor
    class UserBanPublisher{

        @Value("${spring.data.redis.channel.user_ban}")
        private String userBanTopic;

        private final RedisTemplate<String, Object> redisTemplate;

        public void publish(Long message) {
            redisTemplate.convertAndSend(userBanTopic, message);
        }
    }
}