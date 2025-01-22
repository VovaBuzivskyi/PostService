package faang.school.postservice.publisher.kafka;

import faang.school.postservice.config.kafka.KafkaTopicConfig;
import faang.school.postservice.event.comment.CacheCommentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCreateCommentProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicConfig kafkaTopicConfig;

    public void send(CacheCommentEvent event) {
        kafkaTemplate.send(kafkaTopicConfig.commentTopic().name(), event);
        log.info("Event comment created was sent, with comment Id: {}", event.getCommentId());
    }
}
