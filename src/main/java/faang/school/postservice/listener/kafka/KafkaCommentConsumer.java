package faang.school.postservice.listener.kafka;

import faang.school.postservice.event.comment.CacheCommentEvent;
import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaCommentConsumer {

    private final PostService postService;

    @KafkaListener(topics = "${application.kafka.topics.comment-topic-name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(CacheCommentEvent event, Acknowledgment ack) {
        postService.addCommentToPostCache(event.getCommentDto());
        ack.acknowledge();
        log.info("Comment event for comment with id: {}, has been processed",
                event.getCommentDto().getCommentId());
    }
}
