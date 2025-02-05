package faang.school.postservice.service.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostRequestDto;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.exception.PostException;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.publisher.kafka.KafkaCacheUserProducer;
import faang.school.postservice.publisher.kafka.KafkaPostViewProducer;
import faang.school.postservice.publisher.kafka.KafkaPublishPostProducer;
import faang.school.postservice.publisher.redis.impl.RedisMessagePublisher;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.hashtag.HashtagService;
import faang.school.postservice.validator.post.PostValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Spy
    private PostMapper postMapper = Mappers.getMapper(PostMapper.class);

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostValidator postValidator;

    @Mock
    private RedisMessagePublisher redisMessagePublisher;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HashtagService hashtagService;

    @InjectMocks
    private PostService postService;

    @Captor
    private ArgumentCaptor<Post> captor;

    @Mock
    private ThreadPoolConfig threadPoolConfig;

    @Mock
    private KafkaPostViewProducer kafkaPostViewProducer;

    @Mock
    private KafkaPublishPostProducer kafkaPublishPostProducer;

    @Mock
    private KafkaCacheUserProducer kafkaCacheUserProducer;

    @Mock
    private PostCacheService postCacheService;

    @Mock
    private UserContext userContext;

    @Mock
    private UserServiceClient userServiceClient;

    private static final int UNVERIFIED_POSTS_BAN_COUNT = 5;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postService, "batchSize", 10);
        ReflectionTestUtils.setField(postService, "postEventBatchSize", 10);
    }

    @Test
    public void publishPostTest() throws InterruptedException {
        Post post = Post.builder()
                .id(1L)
                .authorId(1L)
                .content("Hello world!")
                .published(false)
                .build();

        List<Long> folowersIds = new ArrayList<>(List.of(10L, 11L));

        Executor executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(2);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(captor.capture())).thenReturn(post);
        when(userContext.getUserId()).thenReturn(1L);
        when(threadPoolConfig.postTaskExecutor()).thenReturn(executor);

        doAnswer(invocation -> {
            latch.countDown();
            return folowersIds;
        }).when(userServiceClient).getFollowersIds(post.getAuthorId());

        doAnswer(invocation -> {
            latch.countDown();
            return new PostCacheDto();
        }).when(postMapper).toPostCacheDto(post);

        postService.publishPost(1L);

        Post createPost = captor.getValue();
        latch.await();

        verify(postCacheService).savePostToCache(post);
        verify(threadPoolConfig).postTaskExecutor();
        verify(userServiceClient).getFollowersIds(post.getAuthorId());
        verify(postMapper).toPostCacheDto(post);
        verify(kafkaPublishPostProducer).send(any());
        verify(kafkaCacheUserProducer).send(any());
        verify(postMapper).toDto(post);

        assertEquals(post.getAuthorId(), createPost.getAuthorId());
        assertEquals(post.getContent(), createPost.getContent());
        assertTrue(createPost.isPublished());
    }

    @Test
    public void publishScheduledPostTest() throws InterruptedException {
        List<Long> folowersIds = new ArrayList<>(List.of(10L, 11L));

        Post post1 = Post.builder()
                .id(1L)
                .authorId(1L)
                .published(false)
                .build();
        Post post2 = Post.builder()
                .id(2L)
                .authorId(1L)
                .published(false)
                .build();
        Post post3 = Post.builder()
                .id(3L)
                .authorId(1L)
                .published(false)
                .build();

        List<Post> mockPosts = new ArrayList<>(List.of(post1, post2, post3));
        Executor executor = Executors.newFixedThreadPool(100);
        ArgumentCaptor<List<Post>> captor = ArgumentCaptor.forClass(List.class);
        CountDownLatch latch = new CountDownLatch(6);

        when(threadPoolConfig.postTaskExecutor()).thenReturn(executor);
        when(postRepository.findReadyToPublish()).thenReturn(mockPosts);
        when(postRepository.saveAll(mockPosts)).thenReturn(mockPosts);
        when(userContext.getUserId()).thenReturn(10L);

        doAnswer(invocation -> {
            latch.countDown();
            return folowersIds;
        }).when(userServiceClient).getFollowersIds(any());

        doAnswer(invocation -> {
            latch.countDown();
            return new PostCacheDto();
        }).when(postMapper).toPostCacheDto(any());

        postService.publishScheduledPosts();

        latch.await();

        verify(kafkaCacheUserProducer, times(3)).send(any());
        verify(postCacheService, times(3)).savePostToCache(any(Post.class));
        verify(userServiceClient, times(3)).getFollowersIds(any());
        verify(postMapper, times(3)).toPostCacheDto(any(Post.class));
        verify(kafkaPublishPostProducer, times(3)).send(any());
        verify(threadPoolConfig, times(4)).postTaskExecutor();
        verify(postRepository, times(1)).findReadyToPublish();
        verify(postRepository, times(1)).saveAll(captor.capture());

        List<List<Post>> capturedPosts = captor.getAllValues();
        assertEquals(1, capturedPosts.size());

        List<Post> allCapturedPosts = new ArrayList<>();
        capturedPosts.forEach(allCapturedPosts::addAll);

        assertTrue(allCapturedPosts.containsAll(mockPosts));
        allCapturedPosts.forEach(post ->
                assertTrue(post.isPublished()));
    }

    @Test
    public void republishPostTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);
        post.setContent("Hello world!");
        post.setPublished(true);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        doThrow(PostException.class).when(postValidator).isPostPublished(post);

        assertThrows(PostException.class, () -> postService.publishPost(1L));
    }

    @Test
    public void createPostTest() {
        PostRequestDto postRequestDto = new PostRequestDto();
        postRequestDto.setAuthorId(1L);
        postRequestDto.setContent("Hello world!");
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);
        post.setContent("Hello world!");
        when(postRepository.save(captor.capture())).thenReturn(post);

        postService.createPost(postRequestDto);

        Post createPost = captor.getValue();

        assertEquals(postRequestDto.getAuthorId(), createPost.getAuthorId());
        assertEquals(postRequestDto.getContent(), createPost.getContent());
        assertFalse(createPost.isPublished());
        assertFalse(createPost.isDeleted());
    }

    @Test
    public void updatePostTest() {
        PostDto postDto = new PostDto();
        postDto.setId(1L);
        postDto.setAuthorId(1L);
        postDto.setContent("Bye world!");
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);
        post.setContent("Hello world!");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(captor.capture())).thenReturn(post);

        postService.updatePost(postDto);

        Post updatePost = captor.getValue();

        assertEquals(postDto.getAuthorId(), updatePost.getAuthorId());
        assertEquals(postDto.getContent(), updatePost.getContent());
    }

    @Test
    public void deletePostTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);
        post.setContent("Hello world!");
        post.setDeleted(false);
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(postRepository.save(captor.capture())).thenReturn(post);

        postService.disablePostById(1L);

        Post deletePost = captor.getValue();

        assertTrue(deletePost.isDeleted());
        assertEquals(post.getAuthorId(), deletePost.getAuthorId());
        assertEquals(post.getContent(), deletePost.getContent());
    }

    @Test
    public void getPostByIdTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);
        post.setContent("Hello world!");
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        PostDto postDto = postService.getPostById(1L);

        verify(kafkaPostViewProducer).send(post.getId());

        assertEquals(post.getId(), postDto.getId());
        assertEquals(post.getAuthorId(), postDto.getAuthorId());
        assertEquals(post.getContent(), postDto.getContent());
    }

    @Test
    public void getNoExistPostByIdTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(1L);
        post.setContent("Hello world!");
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> postService.getPostById(1L));
    }

    @Test
    public void getAllNoPublishPostByUserIdTest() {
        Post firstPost = new Post();
        firstPost.setId(1L);
        firstPost.setAuthorId(1L);
        firstPost.setContent("Hello world!");
        firstPost.setPublished(false);
        firstPost.setDeleted(false);
        firstPost.setCreatedAt(LocalDateTime.now());
        Post secondPost = new Post();
        secondPost.setId(2L);
        secondPost.setAuthorId(1L);
        secondPost.setContent("Bye world!");
        secondPost.setPublished(false);
        secondPost.setDeleted(false);
        secondPost.setCreatedAt(LocalDateTime.now().plusSeconds(1));
        Post thirdPost = new Post();
        thirdPost.setId(3L);
        thirdPost.setAuthorId(1L);
        thirdPost.setContent("Bye, bye!");
        thirdPost.setPublished(true);
        thirdPost.setDeleted(false);
        thirdPost.setCreatedAt(LocalDateTime.now().plusSeconds(2));
        when(postRepository.findByAuthorId(1L)).thenReturn(List.of(firstPost, secondPost, thirdPost));

        List<PostDto> posts = postService.getAllNoPublishPostsByUserId(1L);

        assertEquals(2, posts.size());
        assertEquals(firstPost.getId(), posts.get(1).getId());
        assertEquals(secondPost.getId(), posts.get(0).getId());
    }

    @Test
    public void getAllNoPublishPostByNoExistUserTest() {
        when(postRepository.findByAuthorId(1L)).thenReturn(List.of());

        List<PostDto> posts = postService.getAllNoPublishPostsByUserId(1L);

        assertEquals(0, posts.size());
    }

    @Test
    public void getAllNoPublishPostByProjectIdTest() {
        Post firstPost = new Post();
        firstPost.setId(1L);
        firstPost.setProjectId(1L);
        firstPost.setContent("Hello world!");
        firstPost.setPublished(false);
        firstPost.setDeleted(false);
        firstPost.setCreatedAt(LocalDateTime.now());
        Post secondPost = new Post();
        secondPost.setId(2L);
        secondPost.setProjectId(1L);
        secondPost.setContent("Bye world!");
        secondPost.setPublished(false);
        secondPost.setDeleted(false);
        secondPost.setCreatedAt(LocalDateTime.now().plusSeconds(1));
        Post thirdPost = new Post();
        thirdPost.setId(3L);
        thirdPost.setProjectId(1L);
        thirdPost.setContent("Bye, bye!");
        thirdPost.setPublished(true);
        thirdPost.setDeleted(false);
        thirdPost.setCreatedAt(LocalDateTime.now().plusSeconds(2));
        when(postRepository.findByProjectId(1L)).thenReturn(List.of(firstPost, secondPost, thirdPost));

        List<PostDto> posts = postService.getAllNoPublishPostsByProjectId(1L);

        assertEquals(2, posts.size());
        assertEquals(firstPost.getId(), posts.get(1).getId());
        assertEquals(secondPost.getId(), posts.get(0).getId());
    }

    @Test
    void getAllPostByUserIdTest() {
        long id = 1L;
        List<Like> likes = new ArrayList<>(List.of(new Like(), new Like()));
        Post newestPost = Post.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45))
                .published(true)
                .deleted(false)
                .likes(likes)
                .build();
        Post olderPost = Post.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45).minusMinutes(1))
                .published(true)
                .deleted(false)
                .build();
        Post notPublishedPost = Post.builder()
                .published(false)
                .deleted(false)
                .build();
        Post deletedPost = Post.builder()
                .deleted(true)
                .published(true)
                .build();

        List<Post> posts = new ArrayList<>(List.of(olderPost, newestPost, notPublishedPost, deletedPost));

        PostDto newestPostDto = PostDto.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45))
                .published(true)
                .deleted(false)
                .likesCount(2)
                .build();
        PostDto olderPostDto = PostDto.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45).minusMinutes(1))
                .published(true)
                .deleted(false)
                .build();
        PostDto notPublishedPostDto = PostDto.builder()
                .published(false)
                .deleted(false)
                .build();
        PostDto deletedPostDto = PostDto.builder()
                .published(true)
                .deleted(true)
                .build();
        when(postRepository.findByAuthorIdWithLikes(id)).thenReturn(posts);

        List<PostDto> result = postService.getAllPostsByUserId(id);

        verify(postRepository, times(1)).findByAuthorIdWithLikes(id);
        assertEquals(2, result.size());
        assertEquals(newestPostDto, result.get(0));
        assertEquals(olderPostDto, result.get(1));
        assertEquals(2, result.get(0).getLikesCount());
        assertFalse(result.contains(notPublishedPostDto));
        assertFalse(result.contains(deletedPostDto));
    }

    @Test
    public void getAllPostByProjectIdTest() {
        long id = 1L;
        List<Like> likes = new ArrayList<>(List.of(new Like(), new Like()));
        Post newestPost = Post.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45))
                .published(true)
                .deleted(false)
                .likes(likes)
                .build();
        Post olderPost = Post.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45).minusMinutes(1))
                .published(true)
                .deleted(false)
                .build();
        Post notPublishedPost = Post.builder()
                .published(false)
                .deleted(false)
                .build();
        Post deletedPost = Post.builder()
                .deleted(true)
                .published(true)
                .build();

        List<Post> posts = new ArrayList<>(List.of(olderPost, newestPost, notPublishedPost, deletedPost));

        PostDto newestPostDto = PostDto.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45))
                .published(true)
                .deleted(false)
                .likesCount(2)
                .build();
        PostDto olderPostDto = PostDto.builder()
                .publishedAt(LocalDateTime.of(2023, 11, 13, 14, 30, 45).minusMinutes(1))
                .published(true)
                .deleted(false)
                .build();
        PostDto notPublishedPostDto = PostDto.builder()
                .published(false)
                .deleted(false)
                .build();
        PostDto deletedPostDto = PostDto.builder()
                .published(true)
                .deleted(true)
                .build();
        when(postRepository.findByProjectIdWithLikes(id)).thenReturn(posts);

        List<PostDto> result = postService.getAllPostsByProjectId(id);

        verify(postRepository, times(1)).findByProjectIdWithLikes(id);
        assertEquals(2, result.size());
        assertEquals(newestPostDto, result.get(0));
        assertEquals(olderPostDto, result.get(1));
        assertEquals(2, result.get(0).getLikesCount());
        assertFalse(result.contains(notPublishedPostDto));
        assertFalse(result.contains(deletedPostDto));
    }

    @Test
    public void getPostEntityThrowExceptionTest() {
        long postId = 1L;
        when(postRepository.findById(postId)).thenThrow(EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class,
                () -> postService.getPost(postId));
    }

    @Test
    public void getPostEntityTest() {
        long postId = 1L;
        Post post = new Post();
        post.setId(postId);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        Post result = postService.getPost(postId);

        verify(postRepository, times(1)).findById(postId);
        assertEquals(postId, result.getId());
        assertEquals(post, result);
    }

    @Test
    public void addLikeToPostTest() {
        long id = 1L;
        Post post = Post.builder()
                .id(id).build();
        Like like = Like.builder()
                .id(id).build();
        List<Like> likes = new ArrayList<>();
        post.setLikes(likes);

        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        postService.addLikeToPost(post.getId(), like);

        verify(postRepository).save(post);
        assertTrue(post.getLikes().contains(like));
    }

    @Test
    public void removeLikeFromPostTest() {
        long id = 1L;
        Post post = Post.builder()
                .id(id).build();
        Like like = Like.builder()
                .id(id).build();
        List<Like> likes = new ArrayList<>(List.of(like));
        post.setLikes(likes);
        when(postRepository.findById(id)).thenReturn(Optional.of(post));

        postService.removeLikeFromPost(post.getId(), like);

        verify(postRepository).save(post);
        assertFalse(post.getLikes().contains(like));
    }

    @Test
    public void getPostsWhereVerifiedFalseSuccessTest() throws JsonProcessingException {
        preparePostServiceMock();
        when(postRepository.findAuthorsIdsToBan(UNVERIFIED_POSTS_BAN_COUNT)).thenReturn(List.of(1L));

        postService.getPostsWhereVerifiedFalse();

        verify(postRepository).findAuthorsIdsToBan(UNVERIFIED_POSTS_BAN_COUNT);
        verify(redisMessagePublisher).publish(objectMapper.writeValueAsString(1L));
    }

    @Test
    void getPostsWhereVerifiedFalseNoIdsTest() {
        preparePostServiceMock();

        when(postRepository.findAuthorsIdsToBan(UNVERIFIED_POSTS_BAN_COUNT)).thenReturn(Collections.emptyList());
        postService.getPostsWhereVerifiedFalse();

        verify(postRepository, times(1)).findAuthorsIdsToBan(UNVERIFIED_POSTS_BAN_COUNT);
        verifyNoInteractions(redisMessagePublisher);
    }

    @Test
    void getPostsWhereVerifiedFalseExceptionTest() throws JsonProcessingException {
        preparePostServiceMock();
        List<Long> authorIds = List.of(1L);

        when(postRepository.findAuthorsIdsToBan(UNVERIFIED_POSTS_BAN_COUNT)).thenReturn(authorIds);
        when(objectMapper.writeValueAsString(1L)).thenThrow(new JsonProcessingException("Serialization failed") {
        });

        assertThrows(RuntimeException.class, () -> postService.getPostsWhereVerifiedFalse());

        verify(postRepository, times(1)).findAuthorsIdsToBan(UNVERIFIED_POSTS_BAN_COUNT);
        verify(objectMapper, times(1)).writeValueAsString(1L);
        verifyNoInteractions(redisMessagePublisher);
    }

    private void preparePostServiceMock() {
        ReflectionTestUtils.setField(postService, "unverifiedPostsBanCount", UNVERIFIED_POSTS_BAN_COUNT);
    }

    @Test
    public void isPostExistsReturnTrueTest() {
        long postId = 1L;

        when(postRepository.existsById(postId)).thenReturn(true);

        assertTrue(postService.isPostExists(postId));
    }

    @Test
    public void isPostExistsReturnFalseTest() {
        long postId = 1L;

        when(postRepository.existsById(postId)).thenReturn(false);

        assertFalse(postService.isPostExists(postId));
    }

    @Test
    public void getPostCacheDtoListTest() {
        List<Long> postIds = List.of(1L, 2L);
        Post firstPost = Post.builder().id(1L).build();
        Post secondPost = Post.builder().id(2L).build();

        List<Post> posts = new ArrayList<>(List.of(firstPost, secondPost));

        when(postRepository.findAllById(postIds)).thenReturn(posts);

        List<PostCacheDto> result = postService.getPostCacheDtoList(postIds);

        verify(postMapper).toPostCacheDtoList(posts);

        assertEquals(2, result.size());
    }

    @Test
    public void getBatchNewestPostsPublishedAfterParticularPostTest() {
        List<Long> followeesIds = new ArrayList<>(List.of(1L, 2L));
        long particularPostId = 10L;
        int batchSize = 5;

        Post firstPost = Post.builder().id(1L).build();
        Post secondPost = Post.builder().id(2L).build();

        List<Post> posts = new ArrayList<>(List.of(firstPost, secondPost));

        when(postRepository.findBatchOrderedPostsAfterParticularPostIdInOrderByFolloweesIds(
                followeesIds, particularPostId, batchSize)).thenReturn(posts);

        LinkedHashSet<PostCacheDto> result = postService.
                getBatchNewestPostsPublishedAfterParticularPost(followeesIds, particularPostId, batchSize);

        verify(postMapper).toPostCacheDtoList(posts);

        assertEquals(2, result.size());
    }

    @Test
    public void getBatchPostsFromCacheMissedPostsIsEmptyTest() {
        List<Long> missedPostsIdsInCache = new ArrayList<>();
        List<Long> postIds = new ArrayList<>(Set.of(1L, 2L));

        PostCacheDto firstPost = PostCacheDto.builder().postId(1L).build();
        PostCacheDto secondPost = PostCacheDto.builder().postId(2L).build();

        Set<PostCacheDto> posts = new LinkedHashSet<>(List.of(firstPost, secondPost));

        when(postCacheService.getBatchPostsCaches(postIds, missedPostsIdsInCache)).thenReturn(posts);

        LinkedHashSet<PostCacheDto> result = postService.getBatchPostsFromCache(postIds);

        verifyNoInteractions(postRepository);
        verifyNoInteractions(postMapper);

        assertEquals(2, result.size());
    }

    @Test
    public void getAllPostsIdsPublishedNotLaterDaysAgoTest() {
        long publishedDaysAgo = 10;
        Page<Long> page = new PageImpl<>(List.of(1L, 2L));
        Pageable pagable = mock(Pageable.class);

        when(postRepository.findAllPublishedNotDeletedPostsIdsPublishedNotLaterDaysAgo(
                publishedDaysAgo, pagable)).thenReturn(page);

        Page<Long> result = postService.getAllPostsIdsPublishedNotLaterDaysAgo(publishedDaysAgo, pagable);

        verify(postRepository).findAllPublishedNotDeletedPostsIdsPublishedNotLaterDaysAgo(publishedDaysAgo, pagable);

        assertEquals(page.getContent(), result.getContent());
    }

    @Test
    public void getBatchPostsFromCacheMissedPostsIsNotEmptyTest() {
        List<Long> missedPostsIdsInCache = new ArrayList<>(List.of(2L));
        List<Long> postIds = new ArrayList<>(List.of(1L));

        PostCacheDto firstPost = PostCacheDto.builder()
                .postId(1L)
                .publishedAt(null)
                .build();
        Post secondPost = Post.builder()
                .id(2L)
                .build();

        Set<PostCacheDto> postsFromCache = new LinkedHashSet<>(List.of(firstPost));
        List<Post> postsFromRepository = new ArrayList<>(List.of(secondPost));
        PostCacheDto secondPostCacheDto = PostCacheDto.builder().postId(2L).publishedAt(LocalDateTime.now()).build();

        when(postCacheService.getBatchPostsCaches(eq(postIds), anyList()))
                .thenAnswer(invocation -> {
                    List<Long> missedIds = invocation.getArgument(1);
                    missedIds.addAll(missedPostsIdsInCache);
                    return postsFromCache;
                });

        when(postRepository.findAllById(missedPostsIdsInCache)).thenReturn(postsFromRepository);
        when(postMapper.toPostCacheDtoList(postsFromRepository)).thenReturn(List.of(secondPostCacheDto));

        LinkedHashSet<PostCacheDto> result = postService.getBatchPostsFromCache(postIds);

        assertEquals(2, result.size());
        assertTrue(result.contains(firstPost));
        assertTrue(result.contains(secondPostCacheDto));
    }

    @Test
    public void getPostCacheDtoTest(){
        long postId = 1L;
        Post post1 = Post.builder()
                .id(postId)
                .authorId(1L)
                .published(false)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.ofNullable(post1));

        PostCacheDto result = postService.getPostCacheDto(postId);

        verify(postMapper).toPostCacheDto(post1);

        assertNotNull(result);
        assertEquals(postId, result.getPostId());
    }
}