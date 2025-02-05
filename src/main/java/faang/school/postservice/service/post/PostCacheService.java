package faang.school.postservice.service.post;

import faang.school.postservice.dto.comment.CacheCommentDto;
import faang.school.postservice.mapper.post.PostMapper;
import faang.school.postservice.model.Post;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.repository.cache.PostCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final PostCacheRepository postCacheRepository;
    private final PostMapper postMapper;

    @Value(value = "${feed.comment.quantity-comments-in-post}")
    int commentQuantityInPost;

    public void savePostToCache(Post post) {
        PostCacheDto postCacheDto = postMapper.toPostCacheDto(post);
        savePostToCache(postCacheDto);
    }

    public void saveBatchPostsToCache(Set<PostCacheDto> posts) {
        postCacheRepository.saveBatchPostsToCache(posts);
        log.info("Save posts {} to cache", posts.size());
    }

    public void savePostToCache(PostCacheDto postCacheDto) {
        postCacheRepository.savePostCache(postCacheDto);
        log.info("Saving post with id: {} to post cache", postCacheDto.getPostId());
    }

    public void addPostViewToPostCache(PostCacheDto postCacheDto) {
        updatePostCache(postCacheDto, PostCacheDto::incrementPostViewsCount);
        log.info("Added postView to postCache, for post with id: {}", postCacheDto.getPostId());
    }

    public void addLikeToCachePost(PostCacheDto postCacheDto) {
        updatePostCache(postCacheDto, PostCacheDto::incrementLikesCount);
        log.info("Added like to postCache, for post with id: {}", postCacheDto.getPostId());
    }

    public Set<PostCacheDto> getBatchPostsCaches(List<Long> postIds, List<Long> postsMissedInCache) {
        Set<PostCacheDto> dtos = postCacheRepository.getBatchPostsCaches(new ArrayList<>(postIds), postsMissedInCache);
        log.info("Fetching {} posts from cache", dtos.size());
        return dtos;
    }

    public void addCommentToPostCache(CacheCommentDto commentDto, PostCacheDto postCacheDto) {
        postCacheDto.incrementCommentsCount();
        Set<CacheCommentDto> comments = postCacheDto.getComments();
        if (comments.size() >= commentQuantityInPost) {
            comments.stream().limit(1).forEach(comments::remove);
        }
        comments.add(commentDto);
        postCacheDto.setComments(comments);
        postCacheRepository.savePostCache(postCacheDto);
        log.info("Added comment with id: {} to postCache, for post with id: {}",
                commentDto.getCommentId(), commentDto.getPostId());
    }

    public PostCacheDto getPostCache(long postId) {
        PostCacheDto postCache = postCacheRepository.getPostCache(postId);
        log.info("Fetching post with id: {} from cache", postCache.getPostId());
        return postCache;
    }

    private void updatePostCache(PostCacheDto postCache, Consumer<PostCacheDto> updater) {
        updater.accept(postCache);
        postCacheRepository.savePostCache(postCache);
    }
}
