package faang.school.postservice.validator.post;

import faang.school.postservice.client.ProjectServiceClient;
import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.dto.post.PostRequestDto;
import faang.school.postservice.dto.project.ProjectDto;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.exception.PostException;
import faang.school.postservice.model.Post;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PostValidatorTest {
    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ProjectServiceClient projectServiceClient;

    @InjectMocks
    private PostValidator postValidator;


    @Test
    public void createPostWithoutAuthorAndProjectTest() {
        PostRequestDto postRequestDto = new PostRequestDto();
        postRequestDto.setAuthorId(null);
        postRequestDto.setProjectId(null);

        assertThrows(PostException.class, () -> postValidator.checkCreator(postRequestDto));
    }

    @Test
    public void createPostWithAuthorAndProjectTest() {
        PostRequestDto postRequestDto = new PostRequestDto();
        postRequestDto.setAuthorId(1L);
        postRequestDto.setProjectId(2L);

        assertThrows(PostException.class, () -> postValidator.checkCreator(postRequestDto));
    }

    @Test
    public void createPostWithAuthorTest() {
        PostRequestDto postRequestDto = new PostRequestDto();
        postRequestDto.setAuthorId(1L);
        postRequestDto.setProjectId(null);
        when(userServiceClient.getUser(1L)).thenReturn(new UserDto());

        postValidator.checkCreator(postRequestDto);

        verify(userServiceClient, times(1)).getUser(1L);
    }

    @Test
    public void createPostWithProjectTest() {
        PostRequestDto postRequestDto = new PostRequestDto();
        postRequestDto.setAuthorId(null);
        postRequestDto.setProjectId(2L);
        when(projectServiceClient.getProject(2L)).thenReturn(new ProjectDto());

        assertDoesNotThrow(() -> postValidator.checkCreator(postRequestDto));

        verify(projectServiceClient, times(1)).getProject(2L);
    }

    @Test
    public void checkUpdatePostWithChangeAuthorTest() {
        PostDto postDto = new PostDto();
        postDto.setAuthorId(1L);
        postDto.setProjectId(null);

        Post post = new Post();
        post.setAuthorId(2L);
        post.setProjectId(null);

        assertThrows(PostException.class, () -> postValidator.checkUpdatePost(post, postDto));
    }

    @Test
    public void checkUpdatePostWithChangeProjectTest() {
        PostDto postDto = new PostDto();
        postDto.setAuthorId(null);
        postDto.setProjectId(1L);

        Post post = new Post();
        post.setAuthorId(null);
        post.setProjectId(2L);

        assertThrows(PostException.class, () -> postValidator.checkUpdatePost(post, postDto));
    }

    @Test
    public void checkUpdatePostWithChangeAuthorAndProjectTest() {
        PostDto postDto = new PostDto();
        postDto.setAuthorId(1L);
        postDto.setProjectId(null);

        Post post = new Post();
        post.setAuthorId(null);
        post.setProjectId(2L);

        assertThrows(PostException.class, () -> postValidator.checkUpdatePost(post, postDto));
    }

    @Test
    public void checkUpdatePostTest() {
        PostDto postDto = new PostDto();
        postDto.setAuthorId(1L);
        postDto.setProjectId(null);

        Post post = new Post();
        post.setAuthorId(1L);
        post.setProjectId(null);

        assertDoesNotThrow(() -> postValidator.checkUpdatePost(post, postDto));
    }

    @Test
    void isPostPublished_WhenPostIsNotPublishedTest() {
        Post post = new Post();
        post.setPublished(false);

        assertDoesNotThrow(() -> postValidator.isPostPublished(post));
    }

    @Test
    void isPostPublishedWhenPostIsPublishedTest() {
        Post post = new Post();
        post.setPublished(true);

        assertThrows(PostException.class, () -> postValidator.isPostPublished(post));
    }
}