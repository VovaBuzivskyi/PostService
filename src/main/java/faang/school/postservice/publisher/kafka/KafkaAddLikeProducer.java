package faang.school.postservice.publisher.kafka;

import faang.school.postservice.config.kafka.KafkaTopicConfig;
import faang.school.postservice.event.like.CacheLikeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAddLikeProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicConfig kafkaTopicConfig;

    public void send(CacheLikeEvent event) {
        kafkaTemplate.send(kafkaTopicConfig.likeTopic().name(),event);
        log.info("Event add like was sent successfully for like with id: {}",event.getLikeId());
    }
}
