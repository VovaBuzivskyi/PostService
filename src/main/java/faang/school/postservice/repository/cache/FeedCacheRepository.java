package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.FeedCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedCacheRepository {

    @Cacheable(value = "#{redisCacheProperties.feedsCacheName}", key = "#userId")
    public FeedCacheDto getFeedCacheByUserId(long userId) {
        log.info("No news feed found in cache for user with id: {}", userId);
        return null;
    }

    @CachePut(value = "#{redisCacheProperties.feedsCacheName}", key = "#feedCacheDto.userId")
    public FeedCacheDto saveFeedCache(FeedCacheDto feedCacheDto) {
        return feedCacheDto;
    }

    @CacheEvict(value = "#{redisCacheProperties.feedsCacheName}", key = "#userId")
    public void deleteFeedCache(long userId) {
    }
}
