package faang.school.postservice.publisher;

import faang.school.postservice.event.comment.CommentEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentMessagePublisher implements MessagePublisher<CommentEventDto> {

    @Value("${spring.data.redis.channel.comment}")
    private String commentEventTopic;

    private final RedisTemplate<String, String> redisTemplate;

    public void publish(CommentEventDto commentEventDto) {
        redisTemplate.convertAndSend(commentEventTopic, commentEventDto);
    }
}
