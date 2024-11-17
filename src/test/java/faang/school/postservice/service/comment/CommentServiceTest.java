package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Post;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.validator.comment.CommentValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Spy
    private CommentMapper commentMapper = Mappers.getMapper(CommentMapper.class);

    @Mock
    private CommentValidator commentValidator;

    @Mock
    private PostService postService;

    @InjectMocks
    private CommentService commentService;

    @Test
    public void createCommentTest() {
        CommentDto commentDto = new CommentDto();
        commentDto.setAuthorId(1L);
        commentDto.setContent("Test");
        commentDto.setPostId(1L);

       when(postService.getPostById(commentDto.getPostId())).thenReturn(new PostDto());

        Comment savedComment = new Comment();
        savedComment.setAuthorId(commentDto.getAuthorId());
        savedComment.setContent(commentDto.getContent());
        savedComment.setCreatedAt(LocalDateTime.now());
        savedComment.setPost(new Post());


        doNothing().when(commentValidator).isAuthorExist(commentDto.getAuthorId());
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentDto result = commentService.createComment(commentDto);

        assertEquals(commentDto.getAuthorId(), result.getAuthorId());
        assertEquals(commentDto.getContent(), result.getContent());

        verify(commentValidator, times(1)).isAuthorExist(commentDto.getAuthorId());
        verify(commentMapper, times(1)).toEntity(commentDto);
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentMapper, times(1)).toDto(savedComment);
    }

    @Test
    public void updateComment() {
        Long commentId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("New Test");

        Comment currentComment = new Comment();
        currentComment.setContent("Old content");

        when(commentValidator.getExistingComment(commentId)).thenReturn(currentComment);

        CommentDto result = commentService.updateComment(commentId, commentDto);

        assertNotNull(result);
        assertEquals(commentDto.getContent(), result.getContent());

        verify(commentValidator, times(1)).getExistingComment(commentId);
        verify(commentRepository, times(1)).save(currentComment);
        verify(commentMapper, times(1)).toDto(currentComment);
    }

    @Test
    public void getAllComments() {
        Long postId = 1L;

        when(postService.getPostById(postId)).thenReturn(new PostDto());

        Comment comment1 = new Comment();
        comment1.setCreatedAt(LocalDateTime.of(2024, 11, 11, 12, 0));
        Comment comment2 = new Comment();
        comment2.setCreatedAt(LocalDateTime.of(2024, 10, 11, 12, 0));
        List<Comment> comments = List.of(comment1, comment2);
        when(commentRepository.findAllByPostId(postId)).thenReturn(comments);

        CommentDto commentDto1 = new CommentDto();
        commentDto1.setCreatedAt(comment1.getCreatedAt());
        CommentDto commentDto2 = new CommentDto();
        commentDto2.setCreatedAt(comment2.getCreatedAt());
        List<CommentDto> expectedDtos = List.of(commentDto1, commentDto2);

        when(commentMapper.toDto(comment1)).thenReturn(commentDto1);
        when(commentMapper.toDto(comment2)).thenReturn(commentDto2);

        List<CommentDto> result = commentService.getAllComments(postId);

        assertEquals(expectedDtos, result);

        verify(commentRepository, times(1)).findAllByPostId(postId);
        verify(commentMapper, times(comments.size())).toDto(any());
    }


    @Test
    public void deleteCommentTest() {
        Long authorId = 2L;
        Long commentId = 2L;
        Comment comment = new Comment();
        comment.setAuthorId(commentId);

        doNothing().when(commentValidator).isAuthorExist(authorId);
        when(commentValidator.getExistingComment(commentId)).thenReturn(comment);

        commentService.deleteComment(authorId, commentId);

        verify(commentValidator, times(1)).isAuthorExist(authorId);
        verify(commentValidator, times(1)).getExistingComment(commentId);
        verify(commentRepository, times(1)).deleteById(commentId);
    }

}
