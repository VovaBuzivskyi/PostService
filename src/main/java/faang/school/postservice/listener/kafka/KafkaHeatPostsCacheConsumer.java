package faang.school.postservice.listener.kafka;

import faang.school.postservice.service.feed.FeedHeater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaHeatPostsCacheConsumer {

    private final FeedHeater feedHeater;

    @KafkaListener(topics = "${application.kafka.topics.heat-cache-topic-name}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<Long> event, Acknowledgment ack) {
        feedHeater.heatPostsCache(event);
        log.info("Heat cache consumed for {} posts", event.size());
        ack.acknowledge();
    }
}
