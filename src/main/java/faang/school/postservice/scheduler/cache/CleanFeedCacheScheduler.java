package faang.school.postservice.scheduler.cache;

import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.repository.cache.FeedCacheRepository;
import faang.school.postservice.repository.cache.PostCacheRepository;
import faang.school.postservice.repository.cache.UserCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanFeedCacheScheduler {

    private final PostCacheRepository postCacheRepository;
    private final UserCacheRepository userCacheRepository;
    private final FeedCacheRepository feedCacheRepository;
    private final ThreadPoolConfig poolConfig;

    @Value("${feed.clean-cache.clean-cache-batch}")
    private int batchSize;

    @Value("${feed.clean-cache.post-published-days-ago}")
    private int heaterPostPublishedDaysAgo;

    @Scheduled(cron = "${cron.clean-feed-cache}")
    public void cleanCache() {
        log.info("Start cleaning unused cache");
        poolConfig.newsFeedTaskExecutor().execute(this::cleanFeedAndUsersCache);
        poolConfig.newsFeedTaskExecutor().execute(this::cleanPostsCache);
        log.info("Finish cleaning unused cache");
    }

    private void cleanFeedAndUsersCache() {
        List<UserCacheDto> batchUsers;
        long usersOffset = 0;
        do {
            batchUsers = userCacheRepository.getAllCachesUsers(batchSize, usersOffset);
            batchUsers.forEach(userCacheDto -> {
                try {
                    if (!userCacheDto.isActive()) {
                        userCacheRepository.deleteCacheUserDto(userCacheDto.getUserId());
                        feedCacheRepository.deleteFeedCache(userCacheDto.getUserId());
                        log.info("User with id {} was deleted from cache", userCacheDto.getUserId());
                    }
                } catch (Exception e) {
                    log.error("Failed to clean cache for userId: {}", userCacheDto.getUserId(), e);
                }
            });
            usersOffset += batchSize;
        } while (!batchUsers.isEmpty());
    }

    private void cleanPostsCache() {
        List<PostCacheDto> batchPosts;
        long postsOffset = 0;
        do {
            batchPosts = postCacheRepository.getAllCachesPosts(batchSize, postsOffset);
            batchPosts.forEach(postCacheDto -> {
                LocalDateTime thresholdDate = LocalDateTime.now().minusDays(heaterPostPublishedDaysAgo);
                try {
                    if (postCacheDto.getPublishedAt().isBefore(thresholdDate)) {
                        postCacheRepository.deletePostCache(postCacheDto.getPostId());
                        log.info("Post with id {} was deleted from cache", postCacheDto.getPostId());
                    }
                } catch (Exception e) {
                    log.error("Failed to clean cache for postId: {}", postCacheDto.getPostId(), e);
                }
            });
            postsOffset += batchSize;
        } while (!batchPosts.isEmpty());
    }
}
