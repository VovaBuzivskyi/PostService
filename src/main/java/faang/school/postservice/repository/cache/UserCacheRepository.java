package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.UserCacheDto;
import faang.school.postservice.properties.RedisCacheProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
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
