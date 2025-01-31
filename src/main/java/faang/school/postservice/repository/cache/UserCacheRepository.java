package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class UserCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties prop;

    public UserCacheDto getCacheUserDto(long userId) {
        String key = generateKey(userId);
        Object cachedValue = redisTemplate.opsForValue().get(key);
        return cachedValue != null ? (UserCacheDto) cachedValue : null;
    }

    public void deleteCacheUserDto(long userId) {
        String key = generateKey(userId);
        redisTemplate.delete(key);
    }

    public List<UserCacheDto> getAllCachesUsers(int size, long page) {
        List<UserCacheDto> users = new ArrayList<>();
        long currentIndex = 0;

        try (Cursor<Map.Entry<Object, Object>> cursor = redisTemplate.opsForHash()
                .scan(prop.getUsersCacheName(), ScanOptions.scanOptions().count(size).build())) {

            while (cursor.hasNext()) {
                Map.Entry<Object, Object> entry = cursor.next();

                if (currentIndex++ < page) {
                    continue;
                }

                users.add((UserCacheDto) entry.getValue());
                if (users.size() >= size) {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch users from cache", e);
        }
        return users;
    }

    public List<UserCacheDto> getBatchCacheUserDto(List<Long> userIds, List<Long> usersIdsMissedInCache) {
        List<String> keys = userIds.stream()
                .map(this::generateKey)
                .toList();

        List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(keys);

        for (int i = 0; i < cachedUsers.size(); i++) {
            if (cachedUsers.get(i) == null) {
                usersIdsMissedInCache.add(userIds.get(i));
            }
        }

        return cachedUsers.stream()
                .filter(Objects::nonNull)
                .map(user -> (UserCacheDto) user)
                .toList();
    }

    private String generateKey(long userId) {
        return prop.getUsersCacheName() + userId;
    }
}
