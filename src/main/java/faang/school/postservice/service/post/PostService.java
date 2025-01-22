package faang.school.postservice.service.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.config.kafka.KafkaTopicConfig;
import faang.school.postservice.dto.post.PostCacheDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostRequestDto;
import faang.school.postservice.event.post.PublishPostEvent;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.publisher.redis.impl.RedisMessagePublisher;
import faang.school.postservice.repository.PostRepository;
import faang.school.postservice.service.hashtag.HashtagService;
import faang.school.postservice.validator.post.PostValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserServiceClient userServiceClient;
    private final KafkaTopicConfig kafkaTopicConfig;
    private final RedisCacheManager cacheManager;

    @Value("${post.unverified-posts-ban-count}")
    private Integer unverifiedPostsBanCount;

    @Value("${post.publish-posts.batch-size}")
    private int batchSize;

    @Value("${spring.kafka.event-batch-size}")
    private int eventBatchSize;

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

    public PostDto getPostById(Long postId) {
        Post post = getPost(postId);

        return postMapper.toDto(post);
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

    @Transactional
    public PostDto publishPost(Long postId) {
        Post post = getPost(postId);
        postValidator.isPostPublished(post);
        Post updatedPost = postRepository.save(setPublished(post));
        savePostToCache(updatedPost);
        sendPostCreatedEvent(updatedPost);
        sendCacheUserEvent(updatedPost);
        return postMapper.toDto(updatedPost);
    }

    @Transactional
    @Async("publishingPostsTaskExecutor")
    public void publishScheduledPosts() {
        List<Post> postsToPublish = postRepository.findReadyToPublish();
        List<List<Post>> subLists = divideListToSubLists(postsToPublish, batchSize);

        log.info("Start publishing {} scheduled posts", postsToPublish.size());
        List<CompletableFuture<Void>> futures = subLists.stream()
                .map(sublistOfPosts -> CompletableFuture.runAsync(() -> {
                    sublistOfPosts.forEach(this::setPublished);

                    List<Post> savedPosts = postRepository.saveAll(sublistOfPosts);
                    savedPosts.forEach(post -> {
                        savePostToCache(post);
                        sendPostCreatedEvent(post);
                        sendCacheUserEvent(post);
                    });
                }, poolConfig.publishingPostsTaskExecutor()))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("Finished publishing {} scheduled posts", postsToPublish.size());
    }

    private void savePostToCache(Post post) {
        PostCacheDto postCacheDto = postMapper.toPostCacheDto(post);
        Objects.requireNonNull(cacheManager.getCache("posts")).put(post.getId(), postCacheDto);
        log.info("Saving post with id {} to cache", post.getId());
    }

    private void sendCacheUserEvent(Post post) {
        kafkaTemplate.send(kafkaTopicConfig.cacheUserTopic().name(), post.getAuthorId());
        log.info("Sent event to cache user with id:{}, for post with id {}", post.getAuthorId(), post.getId());
    }

    private void sendPostCreatedEvent(Post post) {
        poolConfig.publishingPostsTaskExecutor().execute(() -> {
            List<Long> followersIds = userServiceClient.getFollowersIds(post.getAuthorId());
            List<List<Long>> subLists = divideListToSubLists(followersIds, eventBatchSize);
            subLists.forEach(ids -> {
                kafkaTemplate.send(kafkaTopicConfig.postTopic().name(), createPostEvent(post, followersIds));
                log.info("Sent event Post created, for post with id {}", post.getId());
            });

        });
        log.info("Post with id {} - published", post.getId());
    }

    private Post setPublished(Post post) {
        post.setPublished(true);
        post.setPublishedAt(LocalDateTime.now());
        return post;
    }

    private PublishPostEvent createPostEvent(Post post, List<Long> followersIds) {
        return PublishPostEvent.builder()
                .postId(post.getId())
                .followeeId(post.getAuthorId())
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