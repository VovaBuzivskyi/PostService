package faang.school.postservice.validator.news_feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.service.post.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsFeedValidatorTest {

    @Mock
    private PostService postService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private NewsFeedValidator newsFeedValidator;

    @Test
    void isUserExistsTest() {
        long userId = 1L;

        when(userServiceClient.isUserExists(userId)).thenReturn(true);

        assertDoesNotThrow(() -> newsFeedValidator.isUserExists(userId));
    }

    @Test
    void isUserExistsThrowsExceptionTest() {
        long userId = 1L;

        when(userServiceClient.isUserExists(userId)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> newsFeedValidator.isUserExists(userId));
    }

    @Test
    void isLastViewedPostExistsWhenLastViewedPostIsNullTest() {
        assertDoesNotThrow(() -> newsFeedValidator.isLastViewedPostExists(null));
    }

    @Test
    void isLastViewedPostExistsWhenPostExistsTest() {
        Long lastViewedPostId = 123L;
        when(postService.isPostExists(lastViewedPostId)).thenReturn(true);

        assertDoesNotThrow(() -> newsFeedValidator.isLastViewedPostExists(lastViewedPostId));
    }

    @Test
    void isLastViewedPostExistsWhenPostDoesNotExistTest() {
        Long lastViewedPostId = 123L;
        when(postService.isPostExists(lastViewedPostId)).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> newsFeedValidator.isLastViewedPostExists(lastViewedPostId),
                "Expected an IllegalStateException if the post does not exist");
    }

    @Test
    void isLastViewedPostShouldBeShownToUserWhenPostBelongsToFolloweesTest() {
        Long userId = 1L;
        Long postId = 100L;
        List<Long> followeeIds = List.of(2L, 3L, 4L);

        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeeIds);
        when(postService.isPostBelongUserFollowees(followeeIds, postId)).thenReturn(true);

        assertDoesNotThrow(() -> newsFeedValidator.isLastViewedPostShouldBeShownToUser(userId, postId));
    }

    @Test
    void isLastViewedPostShouldBeShownToUserWhenPostDoesNotBelongToFolloweesTest() {
        Long userId = 1L;
        Long postId = 100L;
        List<Long> followeeIds = List.of(2L, 3L, 4L);

        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeeIds);
        when(postService.isPostBelongUserFollowees(followeeIds, postId)).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> newsFeedValidator.isLastViewedPostShouldBeShownToUser(userId, postId),
                "Expected an IllegalStateException if the post does not belong to the user's followees");
    }
}