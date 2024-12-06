package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.dto.post.PostDto;
import faang.school.postservice.event.comment.CommentEventDto;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.publisher.CommentMessagePublisher;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    private CommentMessagePublisher commentMessagePublisher;

    @InjectMocks
    private CommentService commentService;

    @Test
    public void createCommentTest() {
        CommentDto commentDto = CommentDto.builder()
                .id(15L)
                .authorId(1L)
                .content("commentContent")
                .postId(1L)
                .build();

        Post post = Post.builder()
                .id(10L)
                .content("postContent")
                .authorId(100L)
                .build();

        CommentEventDto resultCommentEventDto = CommentEventDto.builder()
                .postCreatorId(100L)
                .commenterId(1L)
                .postContent("postContent")
                .commentContent("commentContent")
                .build();

        Comment savedComment = Comment.builder()
                .id(15)
                .authorId(commentDto.getAuthorId())
                .content(commentDto.getContent())
                .post(post)
                .build();

        when(postService.getPost(commentDto.getPostId())).thenReturn(post);
        doNothing().when(commentValidator).isAuthorExist(commentDto.getAuthorId());
        when(commentRepository.save(savedComment)).thenReturn(savedComment);
        doNothing().when(commentMessagePublisher).publish(resultCommentEventDto);

        CommentDto result = commentService.createComment(commentDto);

        assertEquals(commentDto.getAuthorId(), result.getAuthorId());
        assertEquals(commentDto.getContent(), result.getContent());

        verify(commentValidator, times(1)).isAuthorExist(commentDto.getAuthorId());
        verify(commentMapper, times(1)).toEntity(commentDto);
        verify(commentRepository, times(1)).save(any(Comment.class));
        verify(commentMapper, times(1)).toDto(savedComment);
        verify(commentMessagePublisher, times(1)).publish(resultCommentEventDto);
    }

    @Test
    public void updateCommentTest() {
        Long commentId = 1L;
        CommentDto commentDto = new CommentDto();
        commentDto.setContent("New Test");

        Comment currentComment = new Comment();
        currentComment.setId(commentId);
        currentComment.setContent("Old content");

        Comment updatedComment = new Comment();
        updatedComment.setId(commentId);
        updatedComment.setContent("New Test");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(currentComment));
        when(commentRepository.save(currentComment)).thenReturn(updatedComment);

        CommentDto result = commentService.updateComment(commentId, commentDto);

        assertNotNull(result);
        assertEquals(commentDto.getContent(), result.getContent());

        verify(commentRepository, times(1)).findById(commentId);
        verify(commentRepository, times(1)).save(currentComment);
    }

    @Test
    public void getAllCommentsTest() {
        long postId = 1L;

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

        List<CommentDto> result = commentService.getAllCommentsByPostId(postId);

        assertEquals(expectedDtos, result);

        verify(commentRepository, times(1)).findAllByPostId(postId);
        verify(commentMapper, times(comments.size())).toDto(any());
    }

    @Test
    public void deleteCommentTest() {
        Long authorId = 2L;
        Long commentId = 2L;

        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthorId(authorId);

        doNothing().when(commentValidator).isAuthorExist(authorId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(authorId, commentId);

        verify(commentValidator, times(1)).isAuthorExist(authorId);
        verify(commentRepository, times(1)).findById(commentId);
        verify(commentRepository, times(1)).deleteById(commentId);
    }

    @Test
    public void getCommentThrowExceptionTest() {
        long id = 1L;
        when(commentRepository.findById(id)).thenThrow(faang.school.postservice.exception.EntityNotFoundException.class);

        assertThrows(EntityNotFoundException.class,
                () -> commentService.getExistingComment(id));
    }

    @Test
    public void getExistingCommentTest() {
        Long commentId = 1L;
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(new Comment()));
        commentService.getExistingComment(commentId);
        verify(commentRepository, times(1)).findById(commentId);
    }

    @Test
    public void addLikeToCommentTest() {
        long id = 1L;
        Comment comment = Comment.builder()
                .id(id).build();
        Like like = Like.builder()
                .id(id).build();
        List<Like> likes = new ArrayList<>();
        comment.setLikes(likes);

        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));

        commentService.addLikeToComment(comment.getId(), like);

        verify(commentRepository).save(comment);
        assertTrue(comment.getLikes().contains(like));
    }

    @Test
    public void removeLikeFromCommentTest() {
        long id = 1L;
        Comment comment = Comment.builder()
                .id(id).build();
        Like like = Like.builder()
                .id(id).build();
        List<Like> likes = new ArrayList<>(List.of(like));
        comment.setLikes(likes);

        when(commentRepository.findById(id)).thenReturn(Optional.of(comment));

        commentService.removeLikeFromComment(comment.getId(), like);

        verify(commentRepository).save(comment);
        assertFalse(comment.getLikes().contains(like));
    }

    @Test
    public void getAllCommentsNoVerifiedTest() {
        Comment commentFirst = new Comment();
        commentFirst.setAuthorId(1L);
        commentFirst.setVerified(true);
        Comment commentSecond = new Comment();
        commentSecond.setAuthorId(1L);
        commentSecond.setVerified(false);
        Comment commentThird = new Comment();
        commentThird.setAuthorId(1L);
        commentThird.setVerified(false);
        List<Comment> comments = List.of(commentFirst, commentSecond, commentThird);
        when(commentRepository.findAll()).thenReturn(comments);

        List<CommentDto> commentDtos = commentService.getAllCommentsNoVerified();

        assertEquals(2, commentDtos.size());
    }
}