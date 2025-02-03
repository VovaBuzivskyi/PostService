package faang.school.postservice.service.feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.publisher.kafka.KafkaCacheUserProducer;
import faang.school.postservice.publisher.kafka.KafkaHeatCacheProducer;
import faang.school.postservice.service.post.PostCacheService;
import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
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
    private final PostCacheService postCacheService;
    private final NewsFeedService newsFeedService;
    private final UserServiceClient userServiceClient;

    public void startHeatFeedCache() {
        userServiceClient.heatCache();
        Pageable pageable = PageRequest.of(0, eventBatchSize);
        Page<Long> page;
        do {
            page = postService.getAllPostsIdsPublishedNotLaterDaysAgo(publishedDaysAgo, pageable);
            if (page.hasContent()) {
                kafkaHeatPostCacheProducer.send(page.getContent());
            }
            pageable = pageable.next();
        } while (!page.isLast());
    }

    public void heatPostsCache(List<Long> postsIds) {
        List<PostCacheDto> posts = postService.getPostCacheDtoList(postsIds);
        Set<PostCacheDto> postsWithComments = newsFeedService.addLatestCommentsToPosts(new LinkedHashSet<>(posts));
        postCacheService.saveBatchPostsToCache(postsWithComments);

        List<Long> authorIdsToCache = postsWithComments.stream()
                .map(PostCacheDto::getAuthorId)
                .toList();

        kafkaCacheUserProducer.send(authorIdsToCache);
    }
}
