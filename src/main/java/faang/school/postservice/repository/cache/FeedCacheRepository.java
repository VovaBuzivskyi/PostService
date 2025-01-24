package faang.school.postservice.repository.cache;

import faang.school.postservice.dto.feed.FeedCacheDto;
import faang.school.postservice.service.feed.NewsFeedService;
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

    private final NewsFeedService newsFeedService;

    @Cacheable(value = CACHE_NAME, key = "#userId")
    public FeedCacheDto getFeedCacheByUserId(long userId) {
        log.info("No news feed found in cache for user with id: {}", userId);
        return newsFeedService.fillFeed(userId);
    }

    @CachePut(value = CACHE_NAME, key = "#feedCacheDto.userId")
    public FeedCacheDto saveFeedCache(FeedCacheDto feedCacheDto) {
        return feedCacheDto;
    }
}
