package faang.school.postservice.listener.kafka;

import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPostViewConsumer {

    private final PostService postService;

    @KafkaListener(topics = "${spring.kafka.topics.post-views-topic-name}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(String event, Acknowledgment ack) {
        long postId = Long.parseLong(event);
        postService.addPostViewToPostCache(postId);
        ack.acknowledge();
        log.info("Post view added to post cache for post with id: {}", postId);
    }
}
