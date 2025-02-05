package faang.school.postservice.service.post;

import faang.school.postservice.dto.comment.CacheCommentDto;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.mapper.post.PostMapperImpl;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.repository.cache.PostCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostCacheServiceTest {

    @Mock
    private PostCacheRepository postCacheRepository;

    @Spy
    private PostMapper postMapper = new PostMapperImpl();

    @InjectMocks
    private PostCacheService postCacheService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(postCacheService, "commentQuantityInPost", 2);
    }

    @Test
    void savePostToCacheGetPostTest() {
        ArgumentCaptor<PostCacheDto> captor = ArgumentCaptor.forClass(PostCacheDto.class);
        Post post = Post.builder()
                .id(1L)
                .authorId(1L)
                .build();

        postCacheService.savePostToCache(post);

        verify(postMapper).toPostCacheDto(post);
        verify(postCacheRepository).savePostCache(captor.capture());

        PostCacheDto result = captor.getValue();

        assertEquals(1L, result.getPostId());
    }

    @Test
    void saveBatchPostsToCacheTest() {
        PostCacheDto postCacheDto = PostCacheDto.builder().build();
        Set<PostCacheDto> posts = new LinkedHashSet<>(Set.of(postCacheDto));

        postCacheService.saveBatchPostsToCache(posts);

        verify(postCacheRepository).saveBatchPostsToCache(posts);
    }

    @Test
    void savePostToCacheGetDtoTest() {
        PostCacheDto postCacheDto = PostCacheDto.builder().build();

        postCacheService.savePostToCache(postCacheDto);

        verify(postCacheRepository).savePostCache(postCacheDto);
    }

    @Test
    void addPostViewToPostCacheTest() {
        long postId = 1L;
        PostCacheDto postCacheDto = PostCacheDto.builder()
                .postId(postId)
                .postViewsCount(1)
                .build();

        postCacheService.addPostViewToPostCache(postCacheDto);

        verify(postCacheRepository).savePostCache(any());

        assertEquals(2, postCacheDto.getPostViewsCount());
    }

    @Test
    void addLikeToCachePostTest() {
        PostCacheDto postCacheDto = PostCacheDto.builder()
                .postId(1L)
                .likesCount(0)
                .build();

        postCacheService.addLikeToCachePost(postCacheDto);

        verify(postCacheRepository).savePostCache(any());

        assertEquals(1, postCacheDto.getLikesCount());
    }

    @Test
    void getBatchPostsCachesTest() {
        List<Long> postIds = new LinkedList<>();
        List<Long> postsMissedInCache = new LinkedList<>();

        PostCacheDto cacheDto = PostCacheDto.builder().build();
        Set<PostCacheDto> postCacheDtos = new LinkedHashSet<>(Set.of(cacheDto));

        when(postCacheRepository.getBatchPostsCaches(postIds, postsMissedInCache)).thenReturn(postCacheDtos);

        Set<PostCacheDto> result =
                postCacheService.getBatchPostsCaches(new ArrayList<>(postIds), postsMissedInCache);

        assertEquals(postCacheDtos, result);
    }

    @Test
    void addCommentToPostCacheCommentsLessMaxQuantityTest() {
        CacheCommentDto commentDtoToSave = CacheCommentDto.builder().build();
        CacheCommentDto commentDto = CacheCommentDto.builder().build();
        Set<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(commentDto));
        ArgumentCaptor<PostCacheDto> captor = ArgumentCaptor.forClass(PostCacheDto.class);

        PostCacheDto postCacheDto = PostCacheDto.builder()
                .comments(comments)
                .build();

        postCacheService.addCommentToPostCache(commentDtoToSave, postCacheDto);

        verify(postCacheRepository).savePostCache(captor.capture());

        assertEquals(1, postCacheDto.getCommentsCount());
        assertEquals(2, captor.getValue().getComments().size());
    }

    @Test
    void addCommentToPostCacheCommentsEqualsMaxQuantityTest() {
        CacheCommentDto commentDtoToSave = CacheCommentDto.builder().commentId(1L).build();
        CacheCommentDto firstCommentDto = CacheCommentDto.builder().commentId(2L).build();
        CacheCommentDto secondCommentDto = CacheCommentDto.builder().commentId(3L).build();

        Set<CacheCommentDto> comments = new LinkedHashSet<>(Set.of(firstCommentDto, secondCommentDto));
        ArgumentCaptor<PostCacheDto> captor = ArgumentCaptor.forClass(PostCacheDto.class);

        PostCacheDto postCacheDto = PostCacheDto.builder()
                .commentsCount(2)
                .comments(comments)
                .build();

        postCacheService.addCommentToPostCache(commentDtoToSave, postCacheDto);

        verify(postCacheRepository).savePostCache(captor.capture());

        PostCacheDto post = captor.getValue();

        assertEquals(3, postCacheDto.getCommentsCount());
        assertEquals(2, post.getComments().size());
    }

    @Test
    void getPostCacheTest() {
        long postId = 1L;
        PostCacheDto cacheDto = PostCacheDto.builder().build();

        when(postCacheRepository.getPostCache(postId)).thenReturn(cacheDto);

        PostCacheDto result = postCacheService.getPostCache(postId);

        assertEquals(cacheDto, result);
    }
}