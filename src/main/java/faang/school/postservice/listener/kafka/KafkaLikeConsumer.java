package faang.school.postservice.listener.kafka;

import faang.school.postservice.model.cache.PostCacheDto;
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
public class KafkaLikeConsumer {

    private final PostCacheService postCacheService;
    private final NewsFeedService newsFeedService;

    @KafkaListener(topics = "${application.kafka.topics.like-topic-name}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String event, Acknowledgment ack) {
        long postId = Long.parseLong(event);
        PostCacheDto postCacheDto = newsFeedService.getPostCacheDtoWithComments(postId);
        postCacheService.addLikeToCachePost(postCacheDto);
        ack.acknowledge();
        log.info("Post like event handled, for post with id {}", postId);
    }
}
