package faang.school.postservice.publisher.kafka;

import faang.school.postservice.config.kafka.KafkaTopicConfig;
import faang.school.postservice.model.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPostViewProducer {

    private final KafkaTopicConfig kafkaTopicConfig;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(Post post) {
        kafkaTemplate.send(kafkaTopicConfig.profileViewsTopic().name(), post.getId());
        log.info("Post view event was published successfully for Post with id {}", post.getId());
    }
}
