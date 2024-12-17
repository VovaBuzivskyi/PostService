package faang.school.postservice.service.hashtag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.post.PostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class HashtagCacheService {

    private final static String HASHTAGS = "Hashtags";
    private final static String POPULAR_HASHTAGS = "Popular_hashtags";
    private final static String HASHTAGS_SCORES = "Hashtags_scores";
    private final static int COUNT_POPULAR_HASHTAG = 5;

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Map<String, List<PostDto>>> redisTemplateForHashtags;
    private final RedisTemplate<String, String> redisTemplate;

    protected void savePostsGroupedByHashtag(Map<String, Set<PostDto>> posts) {
        redisTemplateForHashtags.opsForHash().putAll(HASHTAGS, posts);

        for (String hashtag : posts.keySet()) {
            redisTemplate.opsForZSet().add(HASHTAGS_SCORES, hashtag, 0);
        }
    }

    protected List<String> getPopularHashtags() {
        return Objects.requireNonNull(
                redisTemplate.opsForZSet().reverseRange(
                        HASHTAGS_SCORES,
                        0,
                        COUNT_POPULAR_HASHTAG - 1
                )).stream()
                .toList();
    }

    protected void updateHashtagCache(Map<String, Set<PostDto>> posts) {
        for (var entry : posts.entrySet()) {
            String key = entry.getKey();
            Set<PostDto> value = entry.getValue();
            if (!redisTemplateForHashtags.opsForHash().hasKey(HASHTAGS, key)) {
                redisTemplateForHashtags.opsForHash().put(HASHTAGS, key, value);
                redisTemplate.opsForZSet().addIfAbsent(HASHTAGS_SCORES, key, 0);
            }

            Object redisData = redisTemplateForHashtags.opsForHash().get(HASHTAGS, key);
            Set<PostDto> redisList = objectMapper.convertValue(redisData, new TypeReference<>() {});
            if (!Objects.equals(redisList, value)) {
                redisTemplateForHashtags.opsForHash().put(HASHTAGS, key, value);
            }
        }
        for (var hashtagFromRedis : redisTemplateForHashtags.opsForHash().keys(HASHTAGS)) {
            if (!posts.containsKey(objectMapper.convertValue(hashtagFromRedis, String.class))) {
                redisTemplateForHashtags.opsForHash().delete(HASHTAGS, hashtagFromRedis);
                redisTemplate.opsForZSet().remove(HASHTAGS_SCORES, hashtagFromRedis);
            }
        }
    }

    protected List<PostDto> getPostsGroupedByHashtag(String hashtag) {
        Object redisData = null;
        if (redisTemplateForHashtags.opsForHash().hasKey(POPULAR_HASHTAGS, hashtag)) {
            redisData = redisTemplateForHashtags.opsForHash().get(POPULAR_HASHTAGS, hashtag);
        } else {
            if (redisTemplateForHashtags.opsForHash().hasKey(HASHTAGS, hashtag)) {
                redisData = redisTemplateForHashtags.opsForHash().get(HASHTAGS, hashtag);
            }
        }
        if (redisData != null) {
            redisTemplate.opsForZSet().incrementScore(HASHTAGS_SCORES, hashtag, 1);
            return objectMapper.convertValue(redisData, new TypeReference<>() {});
        } else {
            throw new NotFoundException("Hashtag not found");
        }
    }

    protected void updatePopularHashtagCache(Map<String, List<PostDto>> posts) {
        redisTemplateForHashtags.opsForHash().getOperations().delete(POPULAR_HASHTAGS);
        redisTemplateForHashtags.opsForHash().putAll(POPULAR_HASHTAGS, posts);
    }

    protected List<PostDto> getPostDtosByHashtag(String hashtag) {
        return objectMapper.convertValue(redisTemplateForHashtags.opsForHash().get(HASHTAGS, hashtag),
                new TypeReference<>() {});
    }

    protected void cleanCache() {
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushDb();
    }
}