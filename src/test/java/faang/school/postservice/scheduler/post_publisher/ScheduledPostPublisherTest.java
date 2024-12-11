package faang.school.postservice.scheduler.post_publisher;

import faang.school.postservice.service.post.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledPostPublisherTest {

    @Mock
    private PostService postService;

    @InjectMocks
    private ScheduledPostPublisher scheduledPostPublisher;

    @Test
    void scheduledPostPublisherTest() {

        scheduledPostPublisher.publishPosts();

        verify(postService, times(1)).publishScheduledPosts();
    }
}