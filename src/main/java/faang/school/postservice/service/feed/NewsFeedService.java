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
import faang.school.postservice.service.post.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsFeedService {

    @Value(value = "${feed.post.quantity-posts-in-feed}")
    private int quantityPostsInFeed;

    @Value(value = "${feed.comment.quantity-comments-in-post}")
    private int quantityCommentsInPost;

    @Value(value = "${feed.page.size}")
    private int pageSize;

    private final PostService postService;
    private final UserServiceClient userServiceClient;
    private final FeedCacheRepository feedCacheRepository;
    private final ThreadPoolConfig poolConfig;
    private final UserCacheRepository userCacheRepository;
    private final CommentService commentService;

    public FeedCacheDto fillFeed(Long userId, int batchSize) {
        log.info("Start filling cache for user with id: {}", userId);
        Set<PostCacheDto> postsSaveToCache = new LinkedHashSet<>();
        FeedCacheDto feedCacheDto = getFeed(userId, batchSize, postsSaveToCache);

        postService.saveBatchPostsToCache(postsSaveToCache);
        feedCacheRepository.saveFeedCache(feedCacheDto);
        log.info("Feed cache for user with id: filled{}", userId);
        return feedCacheDto;
    }

    private FeedCacheDto getFeed(long userId, int batchSize, Set<PostCacheDto> postsSaveToCache) {
        FeedCacheDto feedCacheDto = new FeedCacheDto();
        List<Long> followeesIds = userServiceClient.getFolloweesIds(userId);
        Set<PostCacheDto> newestPosts = getBatchNewestPosts(followeesIds, batchSize);
        Set<Long> postsIds = newestPosts.stream()
                .peek(postsSaveToCache::add)
                .map(PostCacheDto::getPostId)
                .collect(Collectors.toSet());
        feedCacheDto.setPostsIds(postsIds);
        feedCacheDto.setUserId(userId);
        return feedCacheDto;
    }

    public CompletableFuture<Void> addPostToFeeds(PublishPostEvent event) {
        PostCacheDto postToCache = event.getPostDto();

        List<CompletableFuture<Void>> futures = event.getFollowersIds().stream()
                .map(followerId -> CompletableFuture.runAsync(() -> {
                    FeedCacheDto feedCacheDto = feedCacheRepository.getFeedCacheByUserId(followerId);

                    UserCacheDto userDto = userCacheRepository.getCacheUserDto(followerId);
                    if (userDto == null) {
                        userDto = new UserCacheDto();
                        boolean isUserActive = userServiceClient.isUserActive(followerId);
                        userDto.setActive(isUserActive);
                    }

                    if (feedCacheDto == null && userDto.isActive()) {
                        feedCacheDto = fillFeed(followerId, quantityPostsInFeed);
                    } else if (!userDto.isActive()) {
                        log.info("User with id: {} is inactive, feed isn't saving", followerId);
                        return;
                    }
                    Set<Long> postsIds = feedCacheDto.getPostsIds();
                    int replacementNumber = 1;
                    if (postsIds.size() >= quantityPostsInFeed) {
                        postsIds.stream()
                                .limit(replacementNumber)
                                .forEach(postsIds::remove);
                    }
                    postsIds.add(postToCache.getPostId());
                    feedCacheRepository.saveFeedCache(feedCacheDto);
                    postService.savePostToCache(postToCache);
                    log.info("Added post with followerId: {} to feed cache with key: {}",
                            postToCache.getPostId(), followerId);
                }, poolConfig.newsFeedTaskExecutor()))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public NewsFeedResponseDto getNewsFeedBatch(Long lastViewedPostId, long userId) {
        NewsFeedResponseDto newsFeedResponseDto = new NewsFeedResponseDto();
        FeedCacheDto feed = feedCacheRepository.getFeedCacheByUserId(userId);
        boolean isUserExists = userServiceClient.isUserExists(userId);

        if (!isUserExists) {
            throw new IllegalStateException("User with id: " + userId + " does not exist");
        }
        if (lastViewedPostId != null && !postService.isPostExists(lastViewedPostId)) {
            throw new IllegalStateException("Last viewed post with id: %d, does not exist".formatted(lastViewedPostId));
        }

        if (feed == null) {
            Set<PostCacheDto> posts = new LinkedHashSet<>();
            if (lastViewedPostId == null) {
                getFeed(userId, pageSize, posts);
                newsFeedResponseDto.setPosts(posts);

                List<UserCacheDto> postsAuthors = getPostsAuthors(posts);
                newsFeedResponseDto.setPostsAuthors(postsAuthors);
                poolConfig.postTaskExecutor().execute(() -> fillFeed(userId, quantityPostsInFeed));
                return newsFeedResponseDto;
            } else {
                List<Long> followeesIds = userServiceClient.getFolloweesIds(userId);
                posts = getBatchNewestPostsPublishedAfterParticularPost(
                        followeesIds, lastViewedPostId, pageSize);
                List<UserCacheDto> postsAuthors = getPostsAuthors(posts);

                newsFeedResponseDto.setPostsAuthors(postsAuthors);
                newsFeedResponseDto.setPosts(posts);
                return newsFeedResponseDto;
            }
        }

        Set<Long> postIds = feed.getPostsIds();
        Set<PostCacheDto> sortedPosts;
        List<UserCacheDto> postsAuthors;

        if (lastViewedPostId == null) {
            List<Long> batchToBring = postIds.stream()
                    .limit(pageSize)
                    .toList();
            sortedPosts = postService.getBatchPostsFromCache(batchToBring);

            postsAuthors = getPostsAuthors(sortedPosts);
            newsFeedResponseDto.setPosts(sortedPosts);
            newsFeedResponseDto.setPostsAuthors(postsAuthors);
            return newsFeedResponseDto;
        }

        Map.Entry<Integer, Integer> numberToSkipAndRemaining =
                findNumberToSkipAndRemaining(postIds, lastViewedPostId);

        if (numberToSkipAndRemaining.getValue() == null) {
            List<Long> followeesIds = userServiceClient.getFolloweesIds(userId);
            sortedPosts = getBatchNewestPostsPublishedAfterParticularPost(
                    followeesIds, lastViewedPostId, pageSize);

            postsAuthors = getPostsAuthors(sortedPosts);
            newsFeedResponseDto.setPosts(sortedPosts);
            newsFeedResponseDto.setPostsAuthors(postsAuthors);
            return newsFeedResponseDto;
        }

        if (numberToSkipAndRemaining.getValue() >= pageSize) {
            List<Long> batchToBring = postIds.stream()
                    .skip(numberToSkipAndRemaining.getKey())
                    .limit(pageSize)
                    .toList();
            sortedPosts = postService.getBatchPostsFromCache(batchToBring);

            postsAuthors = getPostsAuthors(sortedPosts);
            newsFeedResponseDto.setPosts(sortedPosts);
            newsFeedResponseDto.setPostsAuthors(postsAuthors);
            return newsFeedResponseDto;
        }

        if (numberToSkipAndRemaining.getValue() < pageSize) {
            int quantityToTakeFromCache = postIds.size() - numberToSkipAndRemaining.getKey();
            int quantityToTakeFromRepository = pageSize - quantityToTakeFromCache;
            List<Long> batchToBringFromCache = postIds.stream()
                    .skip(numberToSkipAndRemaining.getKey())
                    .limit(quantityToTakeFromCache)
                    .toList();
            List<Long> followeesIds = userServiceClient.getFolloweesIds(userId);

            Set<PostCacheDto> postsFromRepository = getBatchNewestPostsPublishedAfterParticularPost(
                    followeesIds, lastViewedPostId, quantityToTakeFromRepository);
            Set<PostCacheDto> postsFromCache = postService.getBatchPostsFromCache(batchToBringFromCache);
            sortedPosts = Stream.concat(postsFromRepository.stream(), postsFromCache.stream())
                    .sorted(Comparator.comparing(PostCacheDto::getPublishedAt).reversed())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            postsAuthors = getPostsAuthors(sortedPosts);
            newsFeedResponseDto.setPosts(sortedPosts);
            newsFeedResponseDto.setPostsAuthors(postsAuthors);
            return newsFeedResponseDto;
        }

        return newsFeedResponseDto;
    }

    private Set<PostCacheDto> getBatchNewestPostsPublishedAfterParticularPost(
            List<Long> followeesIds, long particularPostId, int batchSize) {

        LinkedHashSet<PostCacheDto> posts = postService.getBatchNewestPostsPublishedAfterParticularPost(
                followeesIds, particularPostId, batchSize);
        return addLatestCommentsToPosts(posts);
    }

    public Set<PostCacheDto> addLatestCommentsToPosts(Set<PostCacheDto> posts) {
        return posts.stream()
                .peek(postCacheDto -> {
                    LinkedHashSet<CacheCommentDto> comments = commentService.
                            getBatchNewestComments(postCacheDto.getPostId(), quantityCommentsInPost);
                    postCacheDto.setComments(comments);
                }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<PostCacheDto> getBatchNewestPosts(List<Long> followeesIds, int batchSize) {
        Set<PostCacheDto> posts = postService.getBatchNewestPosts(followeesIds, batchSize);
        return addLatestCommentsToPosts(posts);
    }

    private Map.Entry<Integer, Integer> findNumberToSkipAndRemaining(Set<Long> postIds, long postIdToFind) {
        int skipped = 0;
        Integer remaining = 0;
        boolean found = false;

        for (Long postId : postIds) {
            if (!found) {
                skipped++;
            } else {
                remaining++;
            }

            if (postId.equals(postIdToFind)) {
                found = true;
            }
        }

        if (!found) {
            remaining = null;
        }

        return new AbstractMap.SimpleEntry<>(skipped - 1, remaining);
    }

    private List<UserCacheDto> getPostsAuthors(Set<PostCacheDto> posts) {
        List<Long> usersIdsMissedInCache = new ArrayList<>();
        List<Long> postsAuthorsIds = posts.stream().map(PostCacheDto::getAuthorId).toList();
        List<UserCacheDto> usersDto = userCacheRepository.getBatchCacheUserDto(postsAuthorsIds, usersIdsMissedInCache);

        if (!usersIdsMissedInCache.isEmpty()) {
            List<UserCacheDto> users = userServiceClient.getUsersCachesByIds(usersIdsMissedInCache);
            usersDto.addAll(users);
        }
        log.info("Fetched posts authors: {}", postsAuthorsIds.size());
        return usersDto;
    }
}
