package faang.school.postservice.service.feed;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.dto.feed.FeedCacheDto;
import faang.school.postservice.dto.post.PostCacheDto;
import faang.school.postservice.event.post.PublishPostEvent;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.cache.FeedCacheRepository;
import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFeedService {

    @Value("${feed.quantity-posts-in-feed}")
    private int quantityPostsInFeed;

    private final PostService postService;
    private final PostMapper postMapper;
    private final UserServiceClient userServiceClient;
    private final FeedCacheRepository feedCacheRepository;
    private final ThreadPoolConfig poolConfig;

    @Transactional
    public FeedCacheDto fillFeed(Long userId) {
        FeedCacheDto feedCacheDto = new FeedCacheDto();
        List<Long> usersIds = userServiceClient.getFolloweesIds(userId);
        Set<Post> newestPosts = postService.getNewestPosts(usersIds, quantityPostsInFeed);
        Set<PostCacheDto> postCacheDto = newestPosts.stream()
                .map(postMapper::toPostCacheDto)
                .collect(Collectors.toSet());
        feedCacheDto.setPosts(postCacheDto);
        return feedCacheDto;
    }

    public CompletableFuture<Void> addPostToFeeds(PublishPostEvent event) {
        PostCacheDto postToCache = event.getPostDto();

        List<CompletableFuture<Void>> futures = event.getFollowersIds().stream()
                .map(id -> CompletableFuture.runAsync(() -> {
                    FeedCacheDto feedCacheDto = feedCacheRepository.getFeedCacheByUserId(id);
                    Set<PostCacheDto> posts = feedCacheDto.getPosts();
                    if (posts.size() >= quantityPostsInFeed && !posts.isEmpty()) {
                        posts.stream()
                                .limit(1)
                                .forEach(posts::remove);
                    }
                    posts.add(postToCache);
                    log.info("Added post with id: {} to feed cache with key: {}", postToCache.getId(), id);
                    feedCacheRepository.saveFeedCache(feedCacheDto);
                }, poolConfig.newsFeedTaskExecutor()))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
