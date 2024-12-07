package faang.school.postservice.publisher;

import faang.school.postservice.event.file.FilesUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FilesUploadedEventPublisher implements MessagePublisher<FilesUploadedEvent> {

    @Value("${spring.data.redis.channel.files-uploaded}")
    private String filesUploadedChannel;

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(FilesUploadedEvent event) {
        redisTemplate.convertAndSend(filesUploadedChannel, event);
    }
}