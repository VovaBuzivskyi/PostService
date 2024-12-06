package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.redis.publisher.UserBanPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommenterBannerTest {

    @Mock
    private CommentService commentService;

    @Mock
    private UserBanPublisher userBanPublisher;

    @InjectMocks
    private CommenterBanner commenterBanner;

    @Test
    public void banCommenterWhenLessFiveNoVerifiedCommentTest() {
        CommentDto commentFirst = new CommentDto();
        commentFirst.setAuthorId(1L);
        CommentDto commentSecond = new CommentDto();
        commentSecond.setAuthorId(1L);
        CommentDto commentThird = new CommentDto();
        commentThird.setAuthorId(1L);
        List<CommentDto> comments = List.of(commentFirst, commentSecond, commentThird);
        when(commentService.getAllCommentsNoVerified()).thenReturn(comments);

        commenterBanner.banCommenter();

        verify(userBanPublisher, times(0)).publish(any());
    }

    @Test
    public void banCommenterTest() {
        CommentDto commentFirst = new CommentDto();
        commentFirst.setAuthorId(1L);
        CommentDto commentSecond = new CommentDto();
        commentSecond.setAuthorId(1L);
        CommentDto commentThird = new CommentDto();
        commentThird.setAuthorId(1L);
        CommentDto commentFourth = new CommentDto();
        commentFourth.setAuthorId(1L);
        CommentDto commentFifth = new CommentDto();
        commentFifth.setAuthorId(1L);
        CommentDto commentSixth = new CommentDto();
        commentSixth.setAuthorId(1L);
        List<CommentDto> comments = List.of(commentFirst, commentSecond, commentThird,
                commentFourth, commentFifth, commentFifth, commentSixth);
        when(commentService.getAllCommentsNoVerified()).thenReturn(comments);

        commenterBanner.banCommenter();

        verify(userBanPublisher, times(1)).publish(any());
    }

    @Test
    void scheduledAnnotationTest() throws NoSuchMethodException {
        String cron = commenterBanner.getClass()
                .getMethod("banCommenter")
                .getAnnotation(Scheduled.class)
                .cron();

        assertEquals("${cron.check-comments}", cron);
    }
}
