package faang.school.postservice.service.hashtag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.model.Hashtag;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.HashtagRepository;
import faang.school.postservice.service.redis.RedisService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class HashtagService {

    private final static String PATTERN = "#[\\p{L}\\p{N}_]+(?=[\\s.,!?;:()\"']|$)";

    private final HashtagRepository hashtagRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    private void fillCache() {
        Map<String, Set<PostDto>> postsGroupedByHashtag = getMapWithPost();

        redisService.savePostsGroupedByHashtag(postsGroupedByHashtag);
    }

    @Scheduled(cron = "${cron.update-cache}")
    private void updateHashtagCache() {
        Map<String, Set<PostDto>> postsGroupedByHashtag = getMapWithPost();

        redisService.updateHashtagCache(postsGroupedByHashtag);
    }

    private Map<String, Set<PostDto>> getMapWithPost() {
        return findAllHashtagsWithPostIds();
    }

    @Scheduled(cron = "${cron.update-cache}")
    private void updatePopularHashtagCache() {
        List<String> hashtags = redisService.getPopularHashtags();
        Map<String, List<PostDto>> posts = hashtags.stream()
                .collect(Collectors.toMap(
                        string -> string,
                        redisService::getPostDtosByHashtag
                ));
        redisService.updatePopularHashtagCache(posts);
    }

    @PreDestroy
    private void cleaningCache() {
        redisService.cleanCache();
        log.info("Achievement cache cleaned");
    }

    public List<PostDto> getPostsByHashtag(String hashtag) {
        return getPostIdsByHashtag(hashtag.toLowerCase());
    }

    private Map<String, Set<PostDto>> findAllHashtagsWithPostIds() {
        Map<String, Set<PostDto>> groupedResults = new HashMap<>();

        List<Object[]> rawResults = hashtagRepository.findAllHashtagsWithPostIds();
        rawResults.forEach(objects ->
                        groupedResults.put(
                                (String) objects[0],
                                objectMapper.convertValue(objects[1], new TypeReference<>() {}))
                );
        return groupedResults;
    }

    private List<PostDto> getPostIdsByHashtag(String hashtag) {
        return redisService.getPostsGroupedByHashtag(hashtag);
    }

    public void takeHashtags(Post post) {
        Set<Hashtag> hashtags = defineHashtags(post);
        hashtagRepository.saveAll(hashtags);
    }

    public void checkHashtags(Post post) {
        Set<Hashtag> newHashtags = defineHashtags(post);
        Set<Hashtag> hashTags = hashtagRepository.findByPostId(post.getId());
        if (!newHashtags.equals(hashTags)) {
            hashtagRepository.deleteAllByPostId(post.getId());
            hashtagRepository.saveAll(newHashtags);
        }
    }

    private Set<Hashtag> defineHashtags(Post post) {
        Pattern regex = Pattern.compile(PATTERN);
        Long postId = post.getId();
        String text = post.getContent().toLowerCase();
        Matcher matcher = regex.matcher(text);

        return Stream.generate(() -> matcher)
                .takeWhile(Matcher::find)
                .map(m -> new Hashtag(m.group().substring(1), postId))
                .collect(Collectors.toSet());
    }
}
