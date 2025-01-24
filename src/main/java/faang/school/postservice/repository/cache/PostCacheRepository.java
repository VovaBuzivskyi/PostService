package faang.school.postservice.repository.cache;

import faang.school.postservice.model.cache.PostCacheDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PostCacheRepository {

    private static final String CACHE_NAME = "posts";

    @Cacheable(value = CACHE_NAME, key = "#postId")
    public PostCacheDto getPostCache(long postId) {
        log.info("Post with ID {} not found in cache", postId);
        return null;
    }

    @CachePut(value = CACHE_NAME, key = "#postCacheDto.postId")
    public PostCacheDto savePostCache(PostCacheDto postCacheDto) {
        return postCacheDto;
    }
}
