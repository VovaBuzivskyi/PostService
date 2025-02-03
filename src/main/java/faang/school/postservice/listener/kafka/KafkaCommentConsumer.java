package faang.school.postservice.listener.kafka;

import faang.school.postservice.event.comment.CacheCommentEvent;
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
public class KafkaCommentConsumer {

    private final PostCacheService postCacheService;
    private final NewsFeedService newsFeedService;

    @KafkaListener(topics = "${application.kafka.topics.comment-topic-name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(CacheCommentEvent event, Acknowledgment ack) {
        long postId = event.getCommentDto().getPostId();
        PostCacheDto postWithComments = newsFeedService.getPostCacheDtoWithComments(postId);
        postCacheService.addCommentToPostCache(event.getCommentDto(), postWithComments);
        ack.acknowledge();
        log.info("Comment event for comment with id: {}, has been processed",
                event.getCommentDto().getCommentId());
    }
}
