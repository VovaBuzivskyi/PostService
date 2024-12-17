package faang.school.postservice;

import faang.school.postservice.service.album.AlbumService;
import faang.school.postservice.service.comment.CommentService;
import faang.school.postservice.service.hashtag.HashtagCacheService;
import faang.school.postservice.service.hashtag.HashtagService;
import faang.school.postservice.service.like.LikeService;
import faang.school.postservice.service.post.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class PostServiceAppTests {

    @MockBean
    private HashtagCacheService hashtagCacheService;

    @MockBean
    private HashtagService hashtagService;

    @MockBean
    private AlbumService albumService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private LikeService likeService;

    @MockBean
    private PostService postService;

    @Test
    @DisplayName("Test context loading")
    void contextLoads() {

    }
}
