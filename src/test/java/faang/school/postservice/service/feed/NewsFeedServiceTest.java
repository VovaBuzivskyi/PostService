package faang.school.postservice.service.feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.dto.comment.CacheCommentDto;
import faang.school.postservice.dto.news_feed.NewsFeedResponseDto;
import faang.school.postservice.event.post.PublishPostEvent;
import faang.school.postservice.model.cache.FeedCacheDto;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.repository.cache.FeedCacheRepository;
import faang.school.postservice.repository.cache.UserCacheRepository;
import faang.school.postservice.service.comment.CommentService;
import faang.school.postservice.service.post.PostCacheService;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.validator.news_feed.NewsFeedValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsFeedServiceTest {

    @Mock
    private PostService postService;

    @Mock
    private PostCacheService postCacheService;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private FeedCacheRepository feedCacheRepository;

    @Mock
    private ThreadPoolConfig poolConfig;

    @Mock
    private UserCacheRepository userCacheRepository;

    @Mock
    private CommentService commentService;

    @Mock
    private NewsFeedValidator newsFeedValidator;

    @InjectMocks
    private NewsFeedService newsFeedService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newsFeedService, "quantityPostsInFeed", 2);
        ReflectionTestUtils.setField(newsFeedService, "quantityCommentsInPost", 2);
        ReflectionTestUtils.setField(newsFeedService, "pageSize", 2);
    }

    @Test
    void getNewsFeedBatch_FeedPresentInCacheGetPartFeedFromCacheAndPartFromRepositoryTest() {
        ReflectionTestUtils.setField(newsFeedService, "quantityPostsInFeed", 3);
        long userId = 1L;
        long lastViewedPostId = 2L;
        long firstPostId = 2L;
        long secondPostId = 3L;
        int quantityCommentsInPost = 2;

        FeedCacheDto feed = FeedCacheDto.builder()
                .userId(userId)
                .postsIds(new LinkedHashSet<>(Set.of(1L, 2L, 3L)))
                .build();

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(firstPostId)
                .publishedAt(LocalDateTime.now())
                .build();

        PostCacheDto secondPost = PostCacheDto.builder()
                .postId(secondPostId)
                .publishedAt(LocalDateTime.now())
                .build();

        UserCacheDto firstUser = UserCacheDto.builder().build();
        UserCacheDto secondUser = UserCacheDto.builder().build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        when(feedCacheRepository.getFeedCacheByUserId(userId)).thenReturn(feed);
        List<Long> followeesIds = new ArrayList<>();
        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);

        LinkedHashSet<PostCacheDto> postsFromRepository = new LinkedHashSet<>(Set.of(secondPost));
        when(postService.getBatchNewestPostsPublishedAfterParticularPost(
                eq(followeesIds), eq(lastViewedPostId), anyInt())).thenReturn(postsFromRepository);
        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));
        when(commentService.getBatchNewestComments(secondPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        LinkedHashSet<PostCacheDto> postsFromCache = new LinkedHashSet<>(Set.of(firstPost));
        when(postService.getBatchPostsFromCache(anyList())).thenReturn(postsFromCache);
        List<UserCacheDto> users = new ArrayList<>(List.of(firstUser, secondUser));
        when(userCacheRepository.getBatchCacheUserDto(anyList(), anyList())).thenReturn(users);

        NewsFeedResponseDto response = newsFeedService.getNewsFeedBatch(lastViewedPostId, userId);

        assertNotNull(response);
        assertTrue(response.getPosts().containsAll(postsFromRepository));
        assertTrue(response.getPostsAuthors().containsAll(users));
    }

    @Test
    void getNewsFeedBatch_FeedPresentInCacheGetAllFeedFromCacheTest() {
        ReflectionTestUtils.setField(newsFeedService, "quantityPostsInFeed", 3);
        long userId = 1L;
        long lastViewedPostId = 1L;
        long firstPostId = 2L;
        long secondPostId = 3L;

        UserCacheDto firstUser = UserCacheDto.builder().build();
        UserCacheDto secondUser = UserCacheDto.builder().build();

        FeedCacheDto feed = FeedCacheDto.builder()
                .userId(userId)
                .postsIds(new LinkedHashSet<>(Set.of(1L, 2L, 3L)))
                .build();

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(firstPostId)
                .build();
        PostCacheDto secondPost = PostCacheDto.builder()
                .postId(secondPostId)
                .build();

        when(feedCacheRepository.getFeedCacheByUserId(userId)).thenReturn(feed);
        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPost, secondPost));
        when(postService.getBatchPostsFromCache(anyList()))
                .thenReturn(posts);
        List<UserCacheDto> users = new ArrayList<>(List.of(firstUser, secondUser));
        when(userCacheRepository.getBatchCacheUserDto(anyList(), anyList())).thenReturn(users);

        NewsFeedResponseDto response = newsFeedService.getNewsFeedBatch(lastViewedPostId, userId);

        assertNotNull(response);
        assertTrue(response.getPosts().containsAll(posts));
        assertTrue(response.getPostsAuthors().containsAll(users));
    }

    @Test
    void getNewsFeedBatch_FeedPresentInCacheAndLastViewedPostIsNotNullButOutOfPostsIdsListTest() {
        long userId = 1L;
        int batchSize = 2;
        long lastViewedPostId = 10L;
        long firstPostId = 2L;
        long secondPostId = 3L;
        long firstPostAuthorId = 2L;
        long secondPostAuthorId = 3L;
        int quantityCommentsInPost = 2;

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(firstPostId)
                .authorId(firstPostAuthorId)
                .build();
        PostCacheDto secondPost = PostCacheDto.builder()
                .postId(secondPostId)
                .authorId(secondPostAuthorId)
                .build();

        UserCacheDto firstUser = UserCacheDto.builder().build();
        UserCacheDto secondUser = UserCacheDto.builder().build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        FeedCacheDto feed = FeedCacheDto.builder()
                .userId(userId)
                .postsIds(new LinkedHashSet<>(Set.of(1L, 2L)))
                .build();

        when(feedCacheRepository.getFeedCacheByUserId(userId)).thenReturn(feed);
        List<Long> followeesIds = new ArrayList<>();
        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);
        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPost, secondPost));
        when(postService.getBatchNewestPostsPublishedAfterParticularPost(followeesIds, lastViewedPostId, batchSize))
                .thenReturn(posts);
        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));
        when(commentService.getBatchNewestComments(firstPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        when(commentService.getBatchNewestComments(secondPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        List<UserCacheDto> users = new ArrayList<>(List.of(firstUser, secondUser));
        when(userCacheRepository.getBatchCacheUserDto(anyList(), anyList())).thenReturn(users);

        NewsFeedResponseDto response = newsFeedService.getNewsFeedBatch(lastViewedPostId, userId);

        assertNotNull(response);
        assertTrue(response.getPosts().containsAll(posts));
        assertTrue(response.getPostsAuthors().containsAll(users));
    }

    @Test
    void getNewsFeedBatch_FeedPresentInCacheAndLastViewedPostIsNullTest() {
        long userId = 1L;
        Long lastViewedPostId = null;
        long firstPostId = 2L;
        long secondPostId = 3L;

        LinkedHashSet<Long> postsIds = new LinkedHashSet<>(Set.of(firstPostId, secondPostId));
        FeedCacheDto feed = FeedCacheDto.builder()
                .postsIds(postsIds)
                .userId(userId)
                .build();

        PostCacheDto firstPost = PostCacheDto.builder().build();
        PostCacheDto secondPost = PostCacheDto.builder().build();

        UserCacheDto firstUser = UserCacheDto.builder().build();
        UserCacheDto secondUser = UserCacheDto.builder().build();

        when(feedCacheRepository.getFeedCacheByUserId(userId)).thenReturn(feed);

        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPost, secondPost));
        when(postService.getBatchPostsFromCache(new ArrayList<>(postsIds))).thenReturn(posts);

        List<UserCacheDto> users = new ArrayList<>(List.of(firstUser, secondUser));
        when(userCacheRepository.getBatchCacheUserDto(anyList(), anyList()))
                .thenAnswer(invocation -> {
                    List<Long> usersIdsMissedInCache = invocation.getArgument(1);
                    usersIdsMissedInCache.add(3L);
                    return new ArrayList<>(List.of(firstUser));
                });
        when(userServiceClient.getUsersCachesByIds(anyList())).thenReturn(new ArrayList<>(List.of(secondUser)));

        NewsFeedResponseDto response = newsFeedService.getNewsFeedBatch(lastViewedPostId, userId);

        verify(newsFeedValidator).isUserExists(userId);
        verify(newsFeedValidator).isLastViewedPostExists(lastViewedPostId);
        verify(newsFeedValidator).isLastViewedPostShouldBeShownToUser(userId, lastViewedPostId);
        verify(userServiceClient).getUsersCachesByIds(anyList());

        assertNotNull(response);
        assertTrue(response.getPosts().containsAll(posts));
        assertTrue(response.getPostsAuthors().containsAll(users));
    }

    @Test
    void getNewsFeedBatch_FeedNotPresentInCacheAndLastViewedPostIsNotNullTest() {
        long userId = 1L;
        Long lastViewedPostId = 5L;
        int batchSize = 2;
        long firstPostId = 2L;
        long secondPostId = 3L;
        long firstPostAuthorId = 2L;
        long secondPostAuthorId = 3L;
        int quantityCommentsInPost = 2;

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(firstPostId)
                .authorId(firstPostAuthorId)
                .build();
        PostCacheDto secondPost = PostCacheDto.builder()
                .postId(secondPostId)
                .authorId(secondPostAuthorId)
                .build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        UserCacheDto firstUser = UserCacheDto.builder().build();
        UserCacheDto secondUser = UserCacheDto.builder().build();

        List<Long> followeesIds = new ArrayList<>();
        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);
        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPost, secondPost));
        when(postService.getBatchNewestPostsPublishedAfterParticularPost(followeesIds, lastViewedPostId, batchSize))
                .thenReturn(posts);
        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));
        when(commentService.getBatchNewestComments(firstPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        when(commentService.getBatchNewestComments(secondPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        List<UserCacheDto> users = new ArrayList<>(List.of(firstUser, secondUser));
        when(userCacheRepository.getBatchCacheUserDto(anyList(), anyList())).thenReturn(users);

        when(poolConfig.newsFeedTaskExecutor()).thenReturn(Runnable::run);

        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);
        when(postService.getBatchNewestPosts(followeesIds, batchSize)).thenReturn(posts);
        when(commentService.getBatchNewestComments(firstPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        when(commentService.getBatchNewestComments(secondPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);

        NewsFeedResponseDto response = newsFeedService.getNewsFeedBatch(lastViewedPostId, userId);

        verify(newsFeedValidator).isUserExists(userId);
        verify(newsFeedValidator).isLastViewedPostExists(lastViewedPostId);
        verify(newsFeedValidator).isLastViewedPostShouldBeShownToUser(userId, lastViewedPostId);
        verify(postCacheService).saveBatchPostsToCache(posts);
        verify(feedCacheRepository).saveFeedCache(any(FeedCacheDto.class));

        assertNotNull(response);
        assertTrue(response.getPosts().containsAll(posts));
        assertTrue(response.getPostsAuthors().containsAll(users));
    }

    @Test
    void getNewsFeedBatch_FeedNotPresentInCacheAndLastViewedPostIsNullTest() {
        Long lastViewedPostId = null;
        long userId = 1L;
        long firstPostId = 2L;
        long secondPostId = 3L;
        int batchSize = 2;
        int quantityCommentsInPost = 2;
        long firstPostAuthorId = 2L;
        long secondPostAuthorId = 3L;

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(firstPostId)
                .authorId(firstPostAuthorId)
                .build();
        PostCacheDto secondPost = PostCacheDto.builder()
                .postId(secondPostId)
                .authorId(secondPostAuthorId)
                .build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        UserCacheDto firstUser = UserCacheDto.builder().build();
        UserCacheDto secondUser = UserCacheDto.builder().build();

        when(feedCacheRepository.getFeedCacheByUserId(userId)).thenReturn(null);

        List<Long> followeesIds = new ArrayList<>();
        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);
        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPost, secondPost));
        when(postService.getBatchNewestPosts(followeesIds, batchSize)).thenReturn(posts);
        List<UserCacheDto> users = new ArrayList<>(List.of(firstUser, secondUser));
        when(userCacheRepository.getBatchCacheUserDto(anyList(), anyList())).thenReturn(users);
        when(poolConfig.newsFeedTaskExecutor()).thenReturn(Runnable::run);

        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);
        when(postService.getBatchNewestPosts(followeesIds, batchSize)).thenReturn(posts);
        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));
        when(commentService.getBatchNewestComments(firstPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);
        when(commentService.getBatchNewestComments(secondPost.getPostId(), quantityCommentsInPost)).thenReturn(comments);

        NewsFeedResponseDto response = newsFeedService.getNewsFeedBatch(lastViewedPostId, userId);

        verify(newsFeedValidator).isUserExists(userId);
        verify(newsFeedValidator).isLastViewedPostExists(lastViewedPostId);
        verify(newsFeedValidator).isLastViewedPostShouldBeShownToUser(userId, lastViewedPostId);
        verify(postCacheService).saveBatchPostsToCache(posts);
        verify(feedCacheRepository).saveFeedCache(any(FeedCacheDto.class));

        assertNotNull(response);
        assertTrue(response.getPosts().containsAll(posts));
        assertTrue(response.getPostsAuthors().containsAll(users));
    }

    @Test
    void fillFeedTest() {
        long userId = 1;
        int batchSize = 2;
        long firstPostId = 10L;
        long secondPostId = 11L;
        int quantityCommentsInPost = 2;

        PostCacheDto firstPostDto = PostCacheDto.builder()
                .postId(firstPostId)
                .build();
        PostCacheDto secondPostDto = PostCacheDto.builder()
                .postId(secondPostId)
                .build();
        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));
        List<Long> followeesIds = new ArrayList<>();
        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPostDto, secondPostDto));

        when(userServiceClient.getFolloweesIds(userId)).thenReturn(followeesIds);
        when(postService.getBatchNewestPosts(followeesIds, batchSize)).thenReturn(posts);
        when(commentService.getBatchNewestComments(firstPostId, quantityCommentsInPost)).thenReturn(comments);
        when(commentService.getBatchNewestComments(secondPostId, quantityCommentsInPost)).thenReturn(comments);

        FeedCacheDto result = newsFeedService.fillFeed(userId, batchSize);

        verify(postCacheService, times(1)).saveBatchPostsToCache(any());
        verify(feedCacheRepository, times(1)).saveFeedCache(result);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertTrue(result.getPostsIds().contains(firstPostId));
        assertTrue(result.getPostsIds().contains(secondPostId));
    }

    @Test
    void addPostToFeeds_OneNotActiveUserAndFeedCacheDtoPresentsInCacheTest() {
        long firstUserId = 1;
        long secondUserId = 2;

        PostCacheDto postToSave = PostCacheDto.builder()
                .postId(10L)
                .build();

        List<Long> followersIds = new ArrayList<>(List.of(firstUserId, secondUserId));

        PublishPostEvent publishPostEvent = PublishPostEvent.builder()
                .postDto(postToSave)
                .followersIds(followersIds)
                .build();

        LinkedHashSet<Long> postsIds = new LinkedHashSet<>(List.of(1L, 2L));
        FeedCacheDto feedCacheDto = FeedCacheDto.builder()
                .postsIds(postsIds)
                .build();

        UserCacheDto userCacheDto = UserCacheDto.builder()
                .active(true)
                .build();

        when(poolConfig.newsFeedTaskExecutor()).thenReturn(Runnable::run);
        when(feedCacheRepository.getFeedCacheByUserId(firstUserId)).thenReturn(feedCacheDto);
        when(feedCacheRepository.getFeedCacheByUserId(secondUserId)).thenReturn(null);

        when(userCacheRepository.getCacheUserDto(firstUserId)).thenReturn(userCacheDto);
        when(userCacheRepository.getCacheUserDto(secondUserId)).thenReturn(null);
        when(userServiceClient.isUserActive(secondUserId)).thenReturn(false);

        newsFeedService.addPostToFeeds(publishPostEvent);

        verify(feedCacheRepository, times(1)).saveFeedCache(feedCacheDto);
        verifyNoMoreInteractions(feedCacheRepository);
        verify(postCacheService, times(1)).savePostToCache(postToSave);
        verifyNoMoreInteractions(postCacheService);

        assertTrue(feedCacheDto.getPostsIds().contains(10L));
        assertTrue(feedCacheDto.getPostsIds().contains(2L));
        assertFalse(feedCacheDto.getPostsIds().contains(1L));
    }

    @Test
    void addPostToFeeds_FeedCacheDtoNotPresentsInCacheTest() {
        ArgumentCaptor<FeedCacheDto> captor = ArgumentCaptor.forClass(FeedCacheDto.class);
        int quantityCommentsInPost = 2;
        long followerId = 1;
        int batchSize = 2;

        PostCacheDto postToSave = PostCacheDto.builder()
                .postId(10L)
                .build();

        List<Long> followersIds = new ArrayList<>(List.of(followerId));

        PublishPostEvent publishPostEvent = PublishPostEvent.builder()
                .postDto(postToSave)
                .followersIds(followersIds)
                .build();

        UserCacheDto userCacheDto = UserCacheDto.builder()
                .active(true)
                .build();

        List<Long> followeesIds = new ArrayList<>(List.of(2L, 3L));

        PostCacheDto postFromRepository = PostCacheDto.builder()
                .postId(5L)
                .build();

        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(postFromRepository));

        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));

        when(poolConfig.newsFeedTaskExecutor()).thenReturn(Runnable::run);
        when(feedCacheRepository.getFeedCacheByUserId(followerId)).thenReturn(null);
        when(userCacheRepository.getCacheUserDto(followerId)).thenReturn(userCacheDto);

        when(userServiceClient.getFolloweesIds(followerId)).thenReturn(followeesIds);
        when(postService.getBatchNewestPosts(followeesIds, batchSize)).thenReturn(posts);
        when(commentService.getBatchNewestComments(postFromRepository.getPostId(), quantityCommentsInPost))
                .thenReturn(comments);

        newsFeedService.addPostToFeeds(publishPostEvent);

        verify(postCacheService).saveBatchPostsToCache(posts);
        verify(feedCacheRepository, times(2)).saveFeedCache(captor.capture());
        verify(postCacheService).savePostToCache(any(PostCacheDto.class));

        FeedCacheDto feedCacheDto = captor.getValue();

        assertNotNull(feedCacheDto);
        assertTrue(feedCacheDto.getPostsIds().contains(5L));
        assertTrue(feedCacheDto.getPostsIds().contains(10L));
    }

    @Test
    void addLatestCommentsToPostsTest() {
        int quantityCommentsInPost = 2;
        long firstPostId = 10L;
        long secondPostId = 11L;

        PostCacheDto firstPostDto = PostCacheDto.builder()
                .postId(firstPostId)
                .build();

        PostCacheDto secondPostDto = PostCacheDto.builder()
                .postId(secondPostId)
                .build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();
        CacheCommentDto secondComment = CacheCommentDto.builder().build();

        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment, secondComment));
        LinkedHashSet<PostCacheDto> posts = new LinkedHashSet<>(Set.of(firstPostDto, secondPostDto));

        when(commentService.getBatchNewestComments(firstPostId, quantityCommentsInPost)).thenReturn(comments);
        when(commentService.getBatchNewestComments(secondPostId, quantityCommentsInPost)).thenReturn(comments);

        Set<PostCacheDto> result = newsFeedService.addLatestCommentsToPosts(posts);

        assertNotNull(result);
        assertEquals(2, result.size());
        result.forEach(post -> assertTrue(post.getComments().contains(firstComment) &&
                post.getComments().contains(secondComment)));
    }

    @Test
    void getPostCacheDtoWithCommentsPresentsInCacheTest() {
        int quantityCommentsInPost = 2;
        long postId = 1L;
        PostCacheDto post = PostCacheDto.builder()
                .postId(postId)
                .build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();

        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment));

        when(postCacheService.getPostCache(postId)).thenReturn(post);
        when(commentService.getBatchNewestComments(postId, quantityCommentsInPost)).thenReturn(comments);

        PostCacheDto result = newsFeedService.getPostCacheDtoWithComments(postId);

        verifyNoInteractions(postService);

        assertNotNull(result);
        assertEquals(postId, result.getPostId());
        assertEquals(1, result.getComments().size());
        assertTrue(result.getComments().contains(firstComment));
    }

    @Test
    void getPostCacheDtoWithCommentsNotPresentsInCacheTest() {
        int quantityCommentsInPost = 2;
        long postId = 1L;
        PostCacheDto post = PostCacheDto.builder()
                .postId(postId)
                .build();

        CacheCommentDto firstComment = CacheCommentDto.builder().build();

        LinkedHashSet<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstComment));

        when(postCacheService.getPostCache(postId)).thenReturn(null);
        when(postService.getPostCacheDto(postId)).thenReturn(post);
        when(commentService.getBatchNewestComments(postId, quantityCommentsInPost)).thenReturn(comments);

        PostCacheDto result = newsFeedService.getPostCacheDtoWithComments(postId);

        verify(postCacheService, times(1)).getPostCache(postId);
        verify(postService, times(1)).getPostCacheDto(postId);

        assertNotNull(result);
        assertEquals(postId, result.getPostId());
        assertEquals(1, result.getComments().size());
    }
}