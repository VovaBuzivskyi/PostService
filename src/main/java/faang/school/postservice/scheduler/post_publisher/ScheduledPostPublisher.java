package faang.school.postservice.scheduler.post_publisher;

import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledPostPublisher {

    private final PostService postService;

    @Scheduled(cron = "${post.publish-posts.scheduling.cron}")
    public void publishPosts() {
        postService.publishScheduledPosts();
    }
}
