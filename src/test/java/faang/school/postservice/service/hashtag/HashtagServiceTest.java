package faang.school.postservice.service.hashtag;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.model.Hashtag;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.HashtagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HashtagServiceTest {

    @Mock
    private HashtagRepository hashtagRepository;

    @Mock
    private HashtagCacheService hashtagCacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<Set<Hashtag>> captor;

    @InjectMocks
    private HashtagService hashtagService;

    @Test
    public void takeHashtagsTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setContent("Hello #world");
        Hashtag hashtag = new Hashtag();
        hashtag.setPostId(1L);
        hashtag.setHashtag("world");

        hashtagService.takeHashtags(post);

        verify(hashtagRepository, times(1)).saveAll(captor.capture());

        assertTrue(captor.getValue().contains(hashtag));
    }

    @Test
    public void checkHashtagsNoChangesTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setContent("Hello #world");
        Hashtag hashtag = new Hashtag();
        hashtag.setPostId(1L);
        hashtag.setHashtag("world");
        when(hashtagRepository.findByPostId(post.getId())).thenReturn(Set.of(hashtag));

        hashtagService.checkHashtags(post);

        verify(hashtagRepository, times(0)).deleteAllByPostId(post.getId());
        verify(hashtagRepository, times(0)).saveAll(any());
    }

    @Test
    public void checkHashtagsTest() {
        Post post = new Post();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setContent("Hello #world");
        Hashtag oldHashtag = new Hashtag();
        oldHashtag.setPostId(1L);
        oldHashtag.setHashtag("people");
        Hashtag newHashtag = new Hashtag();
        newHashtag.setPostId(1L);
        newHashtag.setHashtag("world");
        when(hashtagRepository.findByPostId(post.getId())).thenReturn(Set.of(oldHashtag));

        hashtagService.checkHashtags(post);

        verify(hashtagRepository, times(1)).deleteAllByPostId(post.getId());
        verify(hashtagRepository, times(1)).saveAll(Set.of(newHashtag));
    }

    @Test
    public void getPostsByHashtagTest() {
        String hashtag = "world";
        PostDto postDto = new PostDto();
        postDto.setId(1L);
        postDto.setAuthorId(2L);
        postDto.setContent("Hello #world");
        when(hashtagCacheService.getPostsGroupedByHashtag(hashtag)).thenReturn(List.of(postDto));

        List<PostDto> postDtos = hashtagService.getPostsByHashtag(hashtag);

        assertEquals(1, postDtos.size());
        assertEquals(postDto, postDtos.get(0));
    }
}
