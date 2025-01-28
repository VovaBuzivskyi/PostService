package faang.school.postservice.service.feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.publisher.kafka.KafkaCacheUserProducer;
import faang.school.postservice.publisher.kafka.KafkaHeatCacheProducer;
import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedHeater {

    @Value(value = "${feed.heater.post-published-days-ago}")
    private long publishedDaysAgo;

    @Value(value = "${feed.heater.event-batch-size}")
    private int eventBatchSize;

    private final KafkaHeatCacheProducer kafkaHeatPostCacheProducer;
    private final KafkaCacheUserProducer kafkaCacheUserProducer;
    private final PostService postService;
    private final NewsFeedService newsFeedService;
    private final UserServiceClient userServiceClient;

    public void startHeatFeedCache() {
        userServiceClient.heatCache();
        int offset = 0;
        List<Long> batch;
        do {
            batch = postService.getAllPostsIdsPublishedNotLaterDaysAgo(publishedDaysAgo, eventBatchSize, offset);
            kafkaHeatPostCacheProducer.send(batch);
            offset += eventBatchSize;
        } while (!batch.isEmpty());
    }

    public void heatPostsCache(List<Long> postsIds) {
        List<PostCacheDto> posts = postService.getPostCacheDtoList(postsIds);
        Set<PostCacheDto> postsWithComments = newsFeedService.addLatestCommentsToPosts(new HashSet<>(posts));
        postService.saveBatchPostsToCache(postsWithComments);

        List<Long> authorIdsToCache = postsWithComments.stream()
                .map(PostCacheDto::getAuthorId)
                .toList();

        kafkaCacheUserProducer.send(authorIdsToCache);
    }
}
