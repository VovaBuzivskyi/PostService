package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final RedissonClient redissonClient;

    public PostCacheDto getPostCache(long postId) {
        String cacheKey = generateKey(postId);
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue == null) {
            return null;
        }
        return (PostCacheDto) cachedValue;
    }

    public void savePostCache(PostCacheDto postCacheDto) {
        String cacheKey = generateKey(postCacheDto.getPostId());
        String lockKey = "lock:post:" + postCacheDto.getPostId();
        RLock lock = redissonClient.getLock(lockKey);
        long ttlInSeconds = Duration.ofHours(prop.getPostsHoursTtl()).toSeconds();

        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                try {
                    redisTemplate.opsForValue().set(cacheKey, postCacheDto, ttlInSeconds, TimeUnit.SECONDS);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Failed to acquire lock for post with id: " + postCacheDto.getPostId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during locking for post with id: " + postCacheDto.getPostId(), e);
        }
    }

    public void deletePostCache(long postId) {
        String cacheKey = generateKey(postId);
        String lockKey = "lock:post:" + postId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                try {
                    redisTemplate.delete(cacheKey);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new IllegalStateException("Failed to acquire lock for post with id: " + postId);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during locking for post with id: " + postId, e);
        }
    }

    public void saveBatchPostsToCache(Set<PostCacheDto> posts) {
        long ttlInSeconds = Duration.ofHours(prop.getPostsHoursTtl()).toSeconds();

        for (PostCacheDto post : posts) {
            String lockKey = "lock:post:" + post.getPostId();
            RLock lock = redissonClient.getLock(lockKey);

            try {
                if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                    try {
                        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                            String key = generateKey(post.getPostId());
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
        List<String> keys = postsIds.stream()
                .map(this::generateKey)
                .toList();
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

    public List<PostCacheDto> getAllCachesPosts(int size, long page) {
        List<PostCacheDto> posts = new ArrayList<>();
        long currentIndex = 0;

        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
                .scan(prop.getPostsCacheName(), ScanOptions.scanOptions().count(size).build())) {

            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();

                if (currentIndex++ < page) {
                    continue;
                }

                posts.add((PostCacheDto) entry.getValue());
                if (posts.size() >= size) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch posts from cache", e);
        }
        return posts;
    }

    private String generateKey(long postId) {
        return prop.getPostsCacheName() + postId;
    }
}
