package faang.school.postservice.service.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.config.context.UserContext;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostRequestDto;
import faang.school.postservice.event.post.PublishPostEvent;
import faang.school.postservice.exception.EntityNotFoundException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final String POST = "Post";

    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final PostValidator postValidator;
    private final RedisMessagePublisher redisMessagePublisher;
    private final ObjectMapper objectMapper;
    private final ThreadPoolConfig poolConfig;
    private final HashtagService hashtagService;
    private final UserServiceClient userServiceClient;
    private final KafkaPostViewProducer kafkaPostViewProducer;
    private final KafkaCacheUserProducer kafkaCacheUserProducer;
    private final KafkaPublishPostProducer kafkaPublishPostProducer;
    private final UserContext userContext;
    private final PostCacheService postCacheService;

    @Value("${post.unverified-posts-ban-count}")
    private Integer unverifiedPostsBanCount;

    @Value("${post.publish-posts.batch-size}")
    private int batchSize;

    @Value("${application.kafka.event-batch-size}")
    private int postEventBatchSize;

    @Value(value = "${feed.comment.quantity-comments-in-post}")
    int commentQuantityInPost;

    public PostDto createPost(PostRequestDto postRequestDtoDto) {
        postValidator.checkCreator(postRequestDtoDto);

        Post createPost = postMapper.toEntity(postRequestDtoDto);
        createPost.setPublished(false);
        createPost.setDeleted(false);

        createPost = postRepository.save(createPost);
        log.info("Post with id {} - created", createPost.getId());
        hashtagService.takeHashtags(createPost);
        return postMapper.toDto(createPost);
    }

    @Transactional
    public PostDto updatePost(PostDto postDto) {
        Post post = getPost(postDto.getId());
        postValidator.checkUpdatePost(post, postDto);

        postMapper.updatePostFromDto(postDto, post);

        hashtagService.checkHashtags(post);
        log.info("Post with id {} - updated", post.getId());
        return postMapper.toDto(postRepository.save(post));
    }

    public void disablePostById(Long postId) {
        Post deletePost = getPost(postId);
        deletePost.setDeleted(true);

        log.info("Post with id {} - deleted", deletePost.getId());
        postRepository.save(deletePost);
    }

    public List<PostDto> getAllNoPublishPostsByUserId(Long userId) {
        List<Post> posts = postRepository.findByAuthorId(userId).stream()
                .filter(post -> !post.isPublished() && !post.isDeleted())
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .toList();

        log.info("Get all drafts posts with author id {}", userId);
        return postMapper.toDto(posts);
    }

    public List<PostDto> getAllNoPublishPostsByProjectId(Long projectId) {
        List<Post> posts = postRepository.findByProjectId(projectId).stream()
                .filter(post -> !post.isPublished() && !post.isDeleted())
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .toList();

        log.info("Get all drafts posts with project id {}", projectId);
        return postMapper.toDto(posts);
    }

    public List<PostDto> getAllPostsByUserId(Long userId) {
        List<Post> posts = postRepository.findByAuthorIdWithLikes(userId).stream()
                .filter(post -> post.isPublished() && !post.isDeleted())
                .sorted(Comparator.comparing(Post::getPublishedAt).reversed())
                .toList();

        log.info("Get all posts with author id {}", userId);
        return postMapper.toDto(posts);
    }

    public List<PostDto> getAllPostsByProjectId(Long projectId) {
        List<Post> posts = postRepository.findByProjectIdWithLikes(projectId).stream()
                .filter(post -> post.isPublished() && !post.isDeleted())
                .sorted(Comparator.comparing(Post::getPublishedAt).reversed())
                .toList();

        log.info("Get all posts with project id {}", projectId);
        return postMapper.toDto(posts);
    }

    public void addLikeToPost(long postId, Like like) {
        Post post = getPost(postId);
        post.getLikes().add(like);

        log.info("Adding like to post with id {}", postId);
        postRepository.save(post);
    }

    public void removeLikeFromPost(long postId, Like like) {
        Post post = getPost(postId);
        post.getLikes().remove(like);

        log.info("Removing like from post with id {}", postId);
        postRepository.save(post);
    }

    public Post getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException(POST, postId));
        log.info("Get post with id {}", postId);
        return post;
    }

    public void getPostsWhereVerifiedFalse() {
        try {
            List<Long> authorIds = postRepository.findAuthorsIdsToBan(unverifiedPostsBanCount);

            if (authorIds.isEmpty()) {
                log.info("No authors to ban");
                return;
            }

            for (Long authorId : authorIds) {
                String message = objectMapper.writeValueAsString(authorId);
                log.info("Message sent to Redis with authorId : {}", authorId);
                redisMessagePublisher.publish(message);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize author ID to JSON", e);
        }
    }

    public boolean isPostBelongUserFollowees(List<Long> foolloweesIds, Long postId) {
        boolean isPostBelong = postRepository.isPostBelongUserFollowees(foolloweesIds, postId);
        log.info(isPostBelong ? "Post with id: %d belong user's followees".formatted(postId) :
                "Post with id: %d doesn't belong user's followees".formatted(postId));
        return isPostBelong;
    }

    @Transactional
    public PostDto getPostById(Long postId) {
        Post post = getPost(postId);
        kafkaPostViewProducer.send(post.getId());
        log.info("Post with id {} - got", postId);
        return postMapper.toDto(post);
    }

    @Transactional
    public PostDto publishPost(Long postId) {
        Post post = getPost(postId);
        postValidator.isPostPublished(post);
        Post updatedPost = postRepository.save(setPublished(post));
        postCacheService.savePostToCache(updatedPost);

        sendPostPublishedEvent(updatedPost);
        kafkaCacheUserProducer.send(new ArrayList<>(List.of(post.getAuthorId())));
        return postMapper.toDto(updatedPost);
    }

    @Transactional
    @Async("postTaskExecutor")
    public void publishScheduledPosts() {
        List<Post> postsToPublish = postRepository.findReadyToPublish();
        List<List<Post>> subLists = divideListToSubLists(postsToPublish, batchSize);

        log.info("Start publishing {} scheduled posts", postsToPublish.size());
        List<CompletableFuture<Void>> futures = subLists.stream()
                .map(sublistOfPosts -> CompletableFuture.runAsync(() -> {
                    sublistOfPosts.forEach(this::setPublished);
                    List<Post> savedPosts = postRepository.saveAll(sublistOfPosts);
                    savedPosts.forEach(post -> {
                        postCacheService.savePostToCache(post);
                        sendPostPublishedEvent(post);
                        kafkaCacheUserProducer.send(new ArrayList<>(List.of(post.getAuthorId())));
                    });
                }, poolConfig.postTaskExecutor()))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Finished publishing {} scheduled posts", postsToPublish.size());
    }

    @Transactional
    public LinkedHashSet<PostCacheDto> getBatchNewestPosts(List<Long> followeesIds, int batchSize) {
        List<Post> post = postRepository.findBatchNewestPostsForUserByFolloweesIds(followeesIds, batchSize);
        log.info("Start getting batch newest Posts: {}, from post repository", post.size());
        return new LinkedHashSet<>(postMapper.toPostCacheDtoList(post));
    }

    @Transactional
    public boolean isPostExists(Long postId) {
        boolean isPostExists = postRepository.existsById(postId);
        log.info(isPostExists ? "Post with id: {} exists" : "Post with id: {} does not exist", postId);
        return isPostExists;
    }

    @Transactional
    public List<PostCacheDto> getPostCacheDtoList(List<Long> postsIds) {
        List<Post> posts = postRepository.findAllById(postsIds);
        log.info("Got {} posts from repository", posts.size());
        return postMapper.toPostCacheDtoList(posts);
    }

    @Transactional
    public PostCacheDto getPostCacheDto(Long postId) {
        Post post = getPost(postId);
        return postMapper.toPostCacheDto(post);
    }

    @Transactional
    public Page<Long> getAllPostsIdsPublishedNotLaterDaysAgo(long publishedDaysAgo, Pageable pageable) {
        Page<Long> postIds = postRepository.findAllPublishedNotDeletedPostsIdsPublishedNotLaterDaysAgo(
                publishedDaysAgo, pageable);
        log.info("Getting published not deleted posts ids: {}, published not later {} days ago",
                postIds.getContent().size(), publishedDaysAgo);
        return postIds;
    }

    @Transactional
    public LinkedHashSet<PostCacheDto> getBatchNewestPostsPublishedAfterParticularPost(
            List<Long> followeesIds, long particularPostId, int batchSize) {
        List<Post> posts = postRepository.findBatchOrderedPostsAfterParticularPostIdInOrderByFolloweesIds(
                followeesIds, particularPostId, batchSize);
        log.info("Start getting batch newest Posts: {}, after particular post with id: {} from post repository",
                posts.size(), particularPostId);
        return new LinkedHashSet<>(postMapper.toPostCacheDtoList(posts));
    }

    @Transactional
    public LinkedHashSet<PostCacheDto> getBatchPostsFromCache(List<Long> postsIds) {
        List<Long> missedPostsIdsInCache = new ArrayList<>();
        Set<PostCacheDto> postsCaches = postCacheService.getBatchPostsCaches(postsIds, missedPostsIdsInCache);
        if (!missedPostsIdsInCache.isEmpty()) {
            List<Post> posts = postRepository.findAllById(missedPostsIdsInCache);
            List<PostCacheDto> postCachesFromRepository = postMapper.toPostCacheDtoList(posts);
            postsCaches.addAll(postCachesFromRepository);
            return postsCaches.stream()
                    .sorted(Comparator.comparing(PostCacheDto::getPublishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        log.info("Got batch postsCachesDtos : {}", postsCaches.size());
        return new LinkedHashSet<>(postsCaches);
    }

    private void sendPostPublishedEvent(Post post) {
        Long userId = userContext.getUserId();
        poolConfig.postTaskExecutor().execute(() -> {
            userContext.setUserId(userId);
            List<Long> followersIds = userServiceClient.getFollowersIds(post.getAuthorId());
            List<List<Long>> subLists = divideListToSubLists(followersIds, postEventBatchSize);
            subLists.forEach(ids -> kafkaPublishPostProducer.send(createPostEvent(post, ids)));
            log.info("Post with id {} - published", post.getId());
        });
    }

    private Post setPublished(Post post) {
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        return post;
    }

    private PublishPostEvent createPostEvent(Post post, List<Long> followersIds) {
        return PublishPostEvent.builder()
                .postDto(postMapper.toPostCacheDto(post))
                .followersIds(followersIds)
                .build();
    }

    private <T> List<List<T>> divideListToSubLists(List<T> list, int batchSize) {
        List<List<T>> subLists = new ArrayList<>();
        int totalSize = list.size();

        for (int i = 0; i < totalSize; i += batchSize) {
            subLists.add(list.subList(i, Math.min(i + batchSize, totalSize)));
        }
        return subLists;
    }
}