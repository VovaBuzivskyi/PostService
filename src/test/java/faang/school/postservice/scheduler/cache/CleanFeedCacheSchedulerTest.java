package faang.school.postservice.scheduler.cache;

import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.repository.cache.FeedCacheRepository;
import faang.school.postservice.repository.cache.PostCacheRepository;
import faang.school.postservice.repository.cache.UserCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanFeedCacheSchedulerTest {

    @Mock
    private PostCacheRepository postCacheRepository;

    @Mock
    private UserCacheRepository userCacheRepository;

    @Mock
    private FeedCacheRepository feedCacheRepository;

    @Mock
    private ThreadPoolConfig poolConfig;

    @InjectMocks
    private CleanFeedCacheScheduler cleanFeedCacheScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanFeedCacheScheduler, "batchSize", 2);
        ReflectionTestUtils.setField(cleanFeedCacheScheduler, "heaterPostPublishedDaysAgo", 5);
    }

    @Test
    void cleanCacheTest() {
        UserCacheDto firsUser = UserCacheDto.builder()
                .userId(1L)
                .active(true)
                .build();

        UserCacheDto secondUser = UserCacheDto.builder()
                .userId(2L)
                .active(false)
                .build();

        List<UserCacheDto> users = new ArrayList<>(List.of(firsUser, secondUser));

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(1L)
                .publishedAt(LocalDateTime.now())
                .build();
        PostCacheDto secondPost = PostCacheDto.builder()
                .postId(2L)
                .publishedAt(LocalDateTime.now().minusDays(10))
                .build();

        List<PostCacheDto> posts = new ArrayList<>(List.of(firstPost, secondPost));

        when(poolConfig.newsFeedTaskExecutor()).thenReturn(Runnable::run);
        when(userCacheRepository.getAllCachesUsers(2, 0)).thenReturn(users);
        when(postCacheRepository.getAllCachesPosts(2, 0)).thenReturn(posts);

        cleanFeedCacheScheduler.cleanCache();

        verify(userCacheRepository, times(1)).deleteCacheUserDto(secondUser.getUserId());
        verify(feedCacheRepository, times(1)).deleteFeedCache(secondUser.getUserId());
        verifyNoMoreInteractions(feedCacheRepository);
        verify(postCacheRepository, times(1)).deletePostCache(secondPost.getPostId());
    }
}