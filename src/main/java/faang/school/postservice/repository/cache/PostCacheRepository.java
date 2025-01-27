package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties prop;

    @Cacheable(value = "#{redisCacheProperties.postsCacheName}", key = "#postId")
    public PostCacheDto getPostCache(long postId) {
        log.info("Post with ID {} not found in cache", postId);
        return null;
    }

    @CachePut(value = "#{redisCacheProperties.postsCacheName}", key = "#postCacheDto.postId")
    public PostCacheDto savePostCache(PostCacheDto postCacheDto) {
        return postCacheDto;
    }

    public void saveBatchPostsToCache(Set<PostCacheDto> posts) {
        String cachePrefix = prop.getPostsCacheName() + "::";
        long ttlInSeconds = Duration.ofHours(prop.getPostsHoursTtl()).toSeconds();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (PostCacheDto post : posts) {
                String key = cachePrefix + post.getPostId();
                redisTemplate.opsForValue().set(key, post, ttlInSeconds, TimeUnit.SECONDS);
            }
            return null;
        });
    }

    public Set<PostCacheDto> getBatchPostsCaches(List<Long> postsIds, List<Long> postsIdsMissedInCache) {
        String cachePrefix = prop.getPostsCacheName() + "::";
        List<String> keys = postsIds.stream()
                .map(postId -> cachePrefix + postId)
                .collect(Collectors.toList());
        List<Object> cachedPosts = redisTemplate.opsForValue().multiGet(keys);

        for (int i = 0; i < cachedPosts.size(); i++) {
            if (cachedPosts.get(i) == null) {
                postsIdsMissedInCache.add(postsIds.get(i));
            }
        }

        return cachedPosts.stream()
                .filter(Objects::nonNull)
                .map(post -> (PostCacheDto) post)
                .collect(Collectors.toSet());
    }
}
