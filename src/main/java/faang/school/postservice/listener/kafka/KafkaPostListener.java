package faang.school.postservice.listener.kafka;

import faang.school.postservice.event.post.PublishPostEvent;
import faang.school.postservice.service.feed.NewsFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPostListener {

    private final NewsFeedService newsFeedService;

    @KafkaListener(topics = "${spring.kafka.topics.post-topic-name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(PublishPostEvent event, Acknowledgment ack) {
        newsFeedService.addPostToFeeds(event)
                .thenRun(ack::acknowledge)
                .exceptionally(ex -> {
                    log.error("Failed to process event for post ID: {}", event.getPostDto().getPostId(), ex);
                    return null;
                });
    }
}
