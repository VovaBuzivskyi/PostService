package faang.school.postservice.publisher.kafka;

import faang.school.postservice.config.kafka.KafkaTopicConfig;
import faang.school.postservice.event.post.PublishPostEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPublishPostProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicConfig kafkaTopicConfig;

    public void send(PublishPostEvent event) {
        kafkaTemplate.send(kafkaTopicConfig.postTopic().name(), event);
        log.info("Sent event Post created, for post with id {}", event.getPostDto().getAuthorId());
    }
}
