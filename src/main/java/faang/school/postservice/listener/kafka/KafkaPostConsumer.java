package faang.school.postservice.listener.kafka;

import faang.school.postservice.event.post.PublishPostEvent;
import faang.school.postservice.service.feed.NewsFeedService;
import faang.school.postservice.service.post.PostCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPostConsumer {

    private final NewsFeedService newsFeedService;
    private final PostCacheService postCacheService;

    @KafkaListener(topics = "${application.kafka.topics.post-topic-name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(PublishPostEvent event, Acknowledgment ack) {
        postCacheService.savePostToCache(event.getPostDto());
        newsFeedService.addPostToFeeds(event)
                .thenRun(ack::acknowledge)
                .exceptionally(ex -> {
                    log.error("Failed to process event for post ID: {}", event.getPostDto().getPostId(), ex);
                    return null;
                });
    }
}
