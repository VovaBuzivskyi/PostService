package faang.school.postservice.service.feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.publisher.kafka.KafkaCacheUserProducer;
import faang.school.postservice.publisher.kafka.KafkaHeatCacheProducer;
import faang.school.postservice.service.post.PostCacheService;
import faang.school.postservice.service.post.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedHeaterTest {

    @Mock
    private KafkaHeatCacheProducer kafkaHeatPostCacheProducer;

    @Mock
    private KafkaCacheUserProducer kafkaCacheUserProducer;

    @Mock
    private PostService postService;

    @Mock
    private PostCacheService postCacheService;

    @Mock
    private NewsFeedService newsFeedService;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private FeedHeater feedHeater;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(feedHeater, "publishedDaysAgo", 2);
        ReflectionTestUtils.setField(feedHeater, "eventBatchSize", 2);
    }

    @Test
    public void feedHeaterTest() {
        List<Long> postIds = new ArrayList<>(List.of(1L, 2L, 3L));
        Pageable pageable = PageRequest.of(0, 2);
        Page<Long> page = new PageImpl<>(postIds);

        when(postService.getAllPostsIdsPublishedNotLaterDaysAgo(2, pageable))
                .thenReturn(page);

        feedHeater.startHeatFeedCache();

        verify(userServiceClient).heatCache();
        verify(kafkaHeatPostCacheProducer).send(postIds);
    }

    @Test
    public void feedHeaterEmptyPageTest() {
        Pageable pageable = PageRequest.of(0, 2);
        Page<Long> page = new PageImpl<>(new ArrayList<>());
        page.isLast();

        when(postService.getAllPostsIdsPublishedNotLaterDaysAgo(2, pageable))
                .thenReturn(page);

        feedHeater.startHeatFeedCache();

        verify(userServiceClient).heatCache();
        verifyNoInteractions(kafkaHeatPostCacheProducer);
    }

    @Test
    public void heatPostsCacheTest() {
        PostCacheDto firstPostCacheDto = PostCacheDto.builder().authorId(10L).build();
        PostCacheDto secontPostCacheDto = PostCacheDto.builder().authorId(20L).build();
        List<Long> postIds = new ArrayList<>(List.of(1L, 2L));
        List<PostCacheDto> posts = new ArrayList<>(List.of(firstPostCacheDto, secontPostCacheDto));

        when(postService.getPostCacheDtoList(postIds)).thenReturn(posts);
        when(newsFeedService.addLatestCommentsToPosts(new HashSet<>(posts))).thenReturn(new LinkedHashSet<>(posts));

        feedHeater.heatPostsCache(postIds);

        verify(postCacheService).saveBatchPostsToCache(new LinkedHashSet<>(posts));
        verify(kafkaCacheUserProducer).send(List.of(10L, 20L));
    }
}