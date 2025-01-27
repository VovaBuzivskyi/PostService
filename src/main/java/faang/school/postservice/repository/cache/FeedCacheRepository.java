package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.FeedCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedCacheRepository {

    private static final String CACHE_NAME = "feeds";

    @Cacheable(value = CACHE_NAME, key = "#userId")
    public FeedCacheDto getFeedCacheByUserId(long userId) {
        log.info("No news feed found in cache for user with id: {}", userId);
        return null;
    }

    @CachePut(value = CACHE_NAME, key = "#feedCacheDto.userId")
    public FeedCacheDto saveFeedCache(FeedCacheDto feedCacheDto) {
        return feedCacheDto;
    }
}
