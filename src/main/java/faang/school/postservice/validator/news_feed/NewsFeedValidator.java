package faang.school.postservice.validator.news_feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NewsFeedValidator {

    private final PostService postService;
    private final UserServiceClient userServiceClient;

    public void isUserExists(long userId) {
        boolean isUserExists = userServiceClient.isUserExists(userId);
        if (!isUserExists) {
            throw new IllegalStateException("User does not exist");
        }
    }

    public void isLastViewedPostExists(Long lastViewedPostId) {
        if (lastViewedPostId != null && !postService.isPostExists(lastViewedPostId)) {
            throw new IllegalStateException("Last viewed post does not exist");
        }
    }

    public void isLastViewedPostShouldBeShownToUser(Long userId, Long postId) {
        List<Long> users = userServiceClient.getFolloweesIds(userId);
        if (!postService.isPostBelongUserFollowees(users, postId)) {
            throw new IllegalStateException("Last viewed post doesn't belong to user's follows");
        }
    }
}
