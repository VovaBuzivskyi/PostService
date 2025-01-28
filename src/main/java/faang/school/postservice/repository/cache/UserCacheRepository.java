package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserCacheRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisCacheProperties prop;

    @Cacheable(value = "#{redisCacheProperties.usersCacheName}", key = "#userId")
    public UserCacheDto getCacheUserDto(long userId) {
        return null;
    }

    @CacheEvict(value = "#{redisCacheProperties.usersCacheName}", key = "#userId")
    public void deleteCacheUserDto(long userId) {
    }

    public List<UserCacheDto> getAllCachesUsersWithPagination(int limit, long offset) {
        return Optional.ofNullable(redisTemplate.keys(prop.getUsersCacheName() + "*"))
                .stream()
                .flatMap(Collection::stream)
                .skip(offset)
                .limit(limit)
                .map(key -> redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .map(user -> (UserCacheDto) user)
                .collect(Collectors.toList());
    }

    public List<UserCacheDto> getBatchCacheUserDto(List<Long> userIds, List<Long> usersIdsMissedInCache) {
        List<String> keys = userIds.stream()
                .map(userId -> prop.getUsersCacheName() + "::" + userId)
                .collect(Collectors.toList());

        List<Object> cachedUsers = redisTemplate.opsForValue().multiGet(keys);

        for (int i = 0; i < cachedUsers.size(); i++) {
            if (cachedUsers.get(i) == null) {
                usersIdsMissedInCache.add(userIds.get(i));
            }
        }

        return cachedUsers.stream()
                .filter(Objects::nonNull)
                .map(user -> (UserCacheDto) user)
                .collect(Collectors.toList());
    }
}
