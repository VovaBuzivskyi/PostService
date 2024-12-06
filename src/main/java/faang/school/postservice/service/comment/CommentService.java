package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CommentDto;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final String COMMENT = "Comment";

    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final CommentValidator commentValidator;
    private final PostService postService;
    private final CommentMessagePublisher commentMessagePublisher;

    @Transactional
    public CommentDto createComment(CommentDto commentDto) {
        Post post = postService.getPost(commentDto.getPostId());
        commentValidator.isAuthorExist(commentDto.getAuthorId());
        Comment commentToSave = commentMapper.toEntity(commentDto);
        commentToSave.setPost(post);
        Comment result = commentRepository.save(commentToSave);

        CommentEventDto commentEventDto = CommentEventDto.builder()
                .postCreatorId(result.getPost().getAuthorId())
                .commenterId(result.getAuthorId())
                .postContent(result.getPost().getContent())
                .commentContent(result.getContent())
                .build();
        commentMessagePublisher.publish(commentEventDto);

        return commentMapper.toDto(result);
    }

    public CommentDto updateComment(Long commentId, CommentDto commentDto) {
        Comment currentComment = getExistingComment(commentId);
        currentComment.setUpdatedAt(LocalDateTime.now());
        currentComment.setContent(commentDto.getContent());
        commentRepository.save(currentComment);
        return commentMapper.toDto(currentComment);
    }

    public List<CommentDto> getAllCommentsByPostId(Long postId) {
        postService.getPostById(postId);
        return commentRepository.findAllByPostId(postId).stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt).reversed())
                .map(commentMapper::toDto)
                .toList();
    }

    public List<CommentDto> getAllCommentsNoVerified() {
        return commentRepository.findAll().stream()
                .filter(comment -> !comment.getVerified())
                .map(commentMapper::toDto)
                .toList();
    }

    public void deleteComment(Long authorId, Long commentId) {
        commentValidator.isAuthorExist(authorId);
        if (getExistingComment(commentId).getAuthorId() == commentId) {
            commentRepository.deleteById(commentId);
        }
    }

    public void addLikeToComment(Long commentId, Like like) {
        Comment comment = getExistingComment(commentId);
        comment.getLikes().add(like);

        log.info("Adding like to comment with ID: {}", comment.getId());
        commentRepository.save(comment);
    }

    public void removeLikeFromComment(Long commentId, Like like) {
        Comment comment = getExistingComment(commentId);
        comment.getLikes().remove(like);

        log.info("Removing like from comment with ID: {}", comment.getId());
        commentRepository.save(comment);
    }

    public Comment getExistingComment(Long commentId) {
        log.info("Searching for comment with ID: {}", commentId);
        return commentRepository.findById(commentId).orElseThrow(() -> {
            log.error("Comment with ID: {} not found", commentId);
            return new EntityNotFoundException(COMMENT, commentId);
        });
    }
}
