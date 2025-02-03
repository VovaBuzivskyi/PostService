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
    private final PostCacheService postCacheService;
    private final UserServiceClient userServiceClient;
    private final FeedCacheRepository feedCacheRepository;
    private final ThreadPoolConfig poolConfig;
    private final UserCacheRepository userCacheRepository;
    private final CommentService commentService;
    private final NewsFeedValidator newsFeedValidator;

    public FeedCacheDto fillFeed(Long userId, int batchSize) {
        log.info("Start filling cache for user with id: {}", userId);
        Set<PostCacheDto> postsSaveToCache = new LinkedHashSet<>();
        FeedCacheDto feedCacheDto = getFeed(userId, batchSize, postsSaveToCache);

        postCacheService.saveBatchPostsToCache(postsSaveToCache);
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
                    postCacheService.savePostToCache(postToCache);
                    log.info("Added post with Id: {} to feed cache with key: {}",
                            postToCache.getPostId(), followerId);
                }, poolConfig.newsFeedTaskExecutor()))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    public NewsFeedResponseDto getNewsFeedBatch(Long lastViewedPostId, long userId) {
        newsFeedValidator.isUserExists(userId);
        newsFeedValidator.isLastViewedPostExists(lastViewedPostId);
        newsFeedValidator.isLastViewedPostShouldBeShownToUser(userId, lastViewedPostId);
        FeedCacheDto feed = feedCacheRepository.getFeedCacheByUserId(userId);

        if (feed == null) {
            if (lastViewedPostId == null) {
                return getFeedFromRepositoryReturnFirstPage(userId);
            } else {
                return getFeedFromRepositoryReturnParticularPage(userId, lastViewedPostId);
            }
        }

        if (lastViewedPostId == null) {
            return getNewsFeedFromCacheReturnFirstPage(feed);
        }

        Set<Long> postsIds = feed.getPostsIds();
        Map.Entry<Integer, Integer> numberToSkipAndRemaining =
                findNumberToSkipAndRemaining(postsIds, lastViewedPostId);

        if (numberToSkipAndRemaining.getValue() == null) {
            return getFeedFromRepositoryReturnParticularPage(userId, lastViewedPostId);
        }

        if (numberToSkipAndRemaining.getValue() >= pageSize) {
            return getNewsFeedFromCacheReturnParticularPage(postsIds, numberToSkipAndRemaining.getKey());
        }

        if (numberToSkipAndRemaining.getValue() < pageSize) {
            return getPartOfFeedFromCacheAndLackPartFromRepository(
                    postsIds, numberToSkipAndRemaining.getKey(), userId, lastViewedPostId);
        }
        throw new InternalError("Error occurred during getting feed");
    }

    public Set<PostCacheDto> addLatestCommentsToPosts(Set<PostCacheDto> posts) {
        return posts.stream()
                .peek(postCacheDto -> {
                    LinkedHashSet<CacheCommentDto> comments = commentService.
                            getBatchNewestComments(postCacheDto.getPostId(), quantityCommentsInPost);
                    postCacheDto.setComments(comments);
                }).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public PostCacheDto getPostCacheDtoWithComments(Long postId) {
        PostCacheDto post;
        post = postCacheService.getPostCache(postId);
        if (post == null) {
            post = postService.getPostCacheDto(postId);
        }
        Set<PostCacheDto> postsWithComments =
                addLatestCommentsToPosts(new LinkedHashSet<>(Set.of(post)));
        PostCacheDto postWithComments = postsWithComments.stream().findFirst().orElseThrow(
                () -> new IllegalStateException("Error adding commentsDtos to postCacheDto with id: " + postId));
        return postWithComments;
    }

    private NewsFeedResponseDto getFeedFromRepositoryReturnFirstPage(long userId) {
        NewsFeedResponseDto newsFeedResponseDto = new NewsFeedResponseDto();
        Set<PostCacheDto> posts = new LinkedHashSet<>();
        getFeed(userId, pageSize, posts);
        newsFeedResponseDto.setPosts(posts);

        List<UserCacheDto> postsAuthors = getPostsAuthors(posts);
        newsFeedResponseDto.setPostsAuthors(postsAuthors);
        poolConfig.postTaskExecutor().execute(() -> fillFeed(userId, quantityPostsInFeed));
        return newsFeedResponseDto;
    }

    private NewsFeedResponseDto getFeedFromRepositoryReturnParticularPage(Long userId, long lastViewedPostId) {
        NewsFeedResponseDto newsFeedResponseDto = new NewsFeedResponseDto();
        List<Long> followeesIds = userServiceClient.getFolloweesIds(userId);
        Set<PostCacheDto> posts = getBatchNewestPostsPublishedAfterParticularPost(followeesIds, lastViewedPostId, pageSize);
        List<UserCacheDto> postsAuthors = getPostsAuthors(posts);

        newsFeedResponseDto.setPostsAuthors(postsAuthors);
        newsFeedResponseDto.setPosts(posts);
        poolConfig.postTaskExecutor().execute(() -> fillFeed(userId, quantityPostsInFeed));
        return newsFeedResponseDto;
    }

    private NewsFeedResponseDto getNewsFeedFromCacheReturnFirstPage(FeedCacheDto feed) {
        NewsFeedResponseDto newsFeedResponseDto = new NewsFeedResponseDto();
        Set<Long> postIds = feed.getPostsIds();

        List<Long> batchToBring = postIds.stream()
                .limit(pageSize)
                .toList();
        Set<PostCacheDto> sortedPosts = postService.getBatchPostsFromCache(batchToBring);

        List<UserCacheDto> postsAuthors = getPostsAuthors(sortedPosts);
        newsFeedResponseDto.setPosts(sortedPosts);
        newsFeedResponseDto.setPostsAuthors(postsAuthors);
        return newsFeedResponseDto;
    }

    private NewsFeedResponseDto getNewsFeedFromCacheReturnParticularPage(Set<Long> postsIds, int postsToSkip) {
        NewsFeedResponseDto newsFeedResponseDto = new NewsFeedResponseDto();
        List<Long> batchToBring = postsIds.stream()
                .skip(postsToSkip)
                .limit(pageSize)
                .toList();

        Set<PostCacheDto> sortedPosts = postService.getBatchPostsFromCache(batchToBring);
        List<UserCacheDto> postsAuthors = getPostsAuthors(sortedPosts);
        newsFeedResponseDto.setPosts(sortedPosts);
        newsFeedResponseDto.setPostsAuthors(postsAuthors);
        return newsFeedResponseDto;
    }

    private NewsFeedResponseDto getPartOfFeedFromCacheAndLackPartFromRepository(
            Set<Long> postsIds, int numbersToSkip, Long userId, Long lastViewedPostId) {
        int quantityToTakeFromCache = postsIds.size() - numbersToSkip;
        int quantityToTakeFromRepository = pageSize - quantityToTakeFromCache;
        List<Long> batchToBringFromCache = postsIds.stream()
                .skip(numbersToSkip)
                .limit(quantityToTakeFromCache)
                .toList();
        List<Long> followeesIds = userServiceClient.getFolloweesIds(userId);

        Set<PostCacheDto> postsFromRepository = getBatchNewestPostsPublishedAfterParticularPost(
                followeesIds, lastViewedPostId, quantityToTakeFromRepository);
        Set<PostCacheDto> postsFromCache = postService.getBatchPostsFromCache(batchToBringFromCache);
        Set<PostCacheDto> sortedPosts = Stream.concat(postsFromRepository.stream(), postsFromCache.stream())
                .sorted(Comparator.comparing(PostCacheDto::getPublishedAt).reversed())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UserCacheDto> postsAuthors = getPostsAuthors(sortedPosts);
        NewsFeedResponseDto newsFeedResponseDto = new NewsFeedResponseDto();
        newsFeedResponseDto.setPosts(sortedPosts);
        newsFeedResponseDto.setPostsAuthors(postsAuthors);
        return newsFeedResponseDto;
    }

    private Set<PostCacheDto> getBatchNewestPostsPublishedAfterParticularPost(
            List<Long> followeesIds, long particularPostId, int batchSize) {

        LinkedHashSet<PostCacheDto> posts = postService.getBatchNewestPostsPublishedAfterParticularPost(
                followeesIds, particularPostId, batchSize);
        return addLatestCommentsToPosts(posts);
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
