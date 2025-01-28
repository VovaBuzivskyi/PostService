package faang.school.postservice.scheduler.cache;

import faang.school.postservice.config.async.ThreadPoolConfig;
import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.repository.cache.FeedCacheRepository;
import faang.school.postservice.repository.cache.PostCacheRepository;
import faang.school.postservice.repository.cache.UserCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

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
        poolConfig.newsFeedTaskExecutor().execute(this::cleanFeedAndUsersCache);
        poolConfig.newsFeedTaskExecutor().execute(this::cleanPostsCache);
    }

    private void cleanFeedAndUsersCache() {
        List<UserCacheDto> batchUsers;
        long usersOffset = 0;
        do {
            batchUsers = userCacheRepository.getAllCachesUsersWithPagination(batchSize, usersOffset);
            batchUsers.forEach(userCacheDto -> {
                if (!userCacheDto.isActive()) {
                    userCacheRepository.deleteCacheUserDto(userCacheDto.getUserId());
                    feedCacheRepository.deleteFeedCache(userCacheDto.getUserId());
                }
            });
            usersOffset += batchSize;
        } while (!batchUsers.isEmpty());
    }

    private void cleanPostsCache() {
        List<PostCacheDto> batchPosts;
        long postsOffset = 0;
        do {
            batchPosts = postCacheRepository.getAllCachesPostsWithPagination(batchSize, postsOffset);
            batchPosts.forEach(postCacheDto -> {
                LocalDateTime thresholdDate = LocalDateTime.now().minusDays(heaterPostPublishedDaysAgo);
                if (postCacheDto.getPublishedAt().isBefore(thresholdDate)) {
                    postCacheRepository.deletePostCache(postCacheDto.getPostId());
                }
            });
            postsOffset += batchSize;
        } while (!batchPosts.isEmpty());
    }
}
