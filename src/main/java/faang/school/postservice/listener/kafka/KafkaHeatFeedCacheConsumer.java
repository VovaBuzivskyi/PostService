package faang.school.postservice.listener.kafka;

import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.service.feed.NewsFeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaHeatFeedCacheConsumer {

    private final NewsFeedService newsFeedService;
    private final ThreadPoolConfig poolConfig;

    @Value(value = "${feed.post.quantity-posts-in-feed}")
    private int quantityPostsInFeed;

    @KafkaListener(topics = "${application.kafka.topics.heat-feed-cache-topic-name}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<Long> usersIdsToFillFeed, Acknowledgment ack) {
        List<CompletableFuture<Void>> futures = usersIdsToFillFeed.stream()
                .map(id -> CompletableFuture.runAsync(() -> newsFeedService.fillFeed(id, quantityPostsInFeed),
                        poolConfig.newsFeedTaskExecutor())).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("News feed fetched.");
        ack.acknowledge();
    }
}
