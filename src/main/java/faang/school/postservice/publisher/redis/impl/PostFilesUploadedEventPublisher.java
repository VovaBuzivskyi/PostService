package faang.school.postservice.publisher.redis.impl;

import faang.school.postservice.event.file.PostFilesUploadedEvent;
import faang.school.postservice.publisher.redis.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostFilesUploadedEventPublisher implements MessagePublisher<PostFilesUploadedEvent> {

    @Value("${spring.data.redis.channel.files-uploaded}")
    private String filesUploadedChannel;

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(PostFilesUploadedEvent event) {
        redisTemplate.convertAndSend(filesUploadedChannel, event);
    }
}