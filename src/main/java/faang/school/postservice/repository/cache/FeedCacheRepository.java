package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.FeedCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedCacheRepository {

    private final RedissonClient redissonClient;

    @Cacheable(value = "#{redisCacheProperties.feedsCacheName}", key = "#userId")
    public FeedCacheDto getFeedCacheByUserId(long userId) {
        log.info("No news feed found in cache for user with id: {}", userId);
        return null;
    }

    @CachePut(value = "#{redisCacheProperties.feedsCacheName}", key = "#feedCacheDto.userId")
    public FeedCacheDto saveFeedCache(FeedCacheDto feedCacheDto) {
        String lockKey = "lock:feed:" + feedCacheDto.getUserId();
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(10, 5, TimeUnit.SECONDS)) {
                try {
                    return feedCacheDto;
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

    @CacheEvict(value = "#{redisCacheProperties.feedsCacheName}", key = "#userId")
    public void deleteFeedCache(long userId) {
    }
}
