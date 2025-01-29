package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties prop;
    private final RedissonClient redissonClient;

    @Cacheable(value = "#{redisCacheProperties.postsCacheName}", key = "#postId")
    public PostCacheDto getPostCache(long postId) {
        log.info("Post with ID {} not found in cache", postId);
        return null;
    }

    @CachePut(value = "#{redisCacheProperties.postsCacheName}", key = "#postCacheDto.postId")
    public PostCacheDto savePostCache(PostCacheDto postCacheDto) {
        String lockKey = "lock:post:" + postCacheDto.getPostId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                try {
                    return postCacheDto;
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Failed to acquire lock");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Error during awaiting locking", e);
        }
    }

    @CacheEvict(value = "#{redisCacheProperties.postsCacheName}", key = "#postId")
    public void deletePostCache(long postId) {
    }

    public void saveBatchPostsToCache(Set<PostCacheDto> posts) {
        String cachePrefix = prop.getPostsCacheName() + "::";
        long ttlInSeconds = Duration.ofHours(prop.getPostsHoursTtl()).toSeconds();

        for (PostCacheDto post : posts) {
            String lockKey = "lock:post:" + post.getPostId();
            RLock lock = redissonClient.getLock(lockKey);

            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                            String key = cachePrefix + post.getPostId();
                            redisTemplate.opsForValue().set(key, post, ttlInSeconds, TimeUnit.SECONDS);
                            return null;
                        });
                    } finally {
                        lock.unlock();
                    }
                } else {
                    throw new IllegalStateException("Failed to acquire lock for post with id: " + post.getPostId());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error during awaiting locking for post with id: " + post.getPostId(), e);
            }
        }
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

    public List<PostCacheDto> getAllCachesPostsWithPagination(int limit, long offset) {
        return Optional.ofNullable(redisTemplate.keys(prop.getPostsCacheName() + "*"))
                .stream()
                .flatMap(Collection::stream)
                .skip(offset)
                .limit(limit)
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .map(post -> (PostCacheDto) post)
                .collect(Collectors.toList());
    }
}
