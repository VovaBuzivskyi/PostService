package faang.school.postservice.service.comment;

import faang.school.postservice.dto.comment.CacheCommentDto;
import faang.school.postservice.dto.comment.CommentDto;
import faang.school.postservice.event.comment.CacheCommentEvent;
import faang.school.postservice.event.comment.CommentEventDto;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.mapper.CommentMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.publisher.kafka.KafkaCacheUserProducer;
import faang.school.postservice.publisher.kafka.KafkaCreateCommentProducer;
import faang.school.postservice.publisher.redis.impl.CommentMessagePublisher;
import faang.school.postservice.repository.CommentRepository;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.validator.comment.CommentValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

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
    private final KafkaCreateCommentProducer kafkaCreateCommentProducer;
    private final KafkaCacheUserProducer kafkaCacheUserProducer;

    @Transactional
    public CommentDto createComment(CommentDto commentDto) {
        Post post = postService.getPost(commentDto.getPostId());
        commentValidator.isAuthorExist(commentDto.getAuthorId());
        Comment commentToSave = commentMapper.toEntity(commentDto);
        commentToSave.setPost(post);
        Comment savedComment = commentRepository.save(commentToSave);

        sendRedisCommentEvent(savedComment);
        kafkaCreateCommentProducer.send(CacheCommentEvent.builder()
                .commentDto(commentMapper.toCacheCommentDto(savedComment))
                .build());
        kafkaCacheUserProducer.send(new ArrayList<>(List.of(savedComment.getAuthorId())));

        log.info("Created comment with id: {}", savedComment.getId());
        return commentMapper.toDto(savedComment);
    }

    public CommentDto updateComment(Long commentId, CommentDto commentDto) {
        Comment currentComment = getComment(commentId);
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
        if (getComment(commentId).getAuthorId() == commentId) {
            commentRepository.deleteById(commentId);
        }
    }

    public void addLikeToComment(Long commentId, Like like) {
        Comment comment = getComment(commentId);
        comment.getLikes().add(like);

        log.info("Adding like to comment with ID: {}", comment.getId());
        commentRepository.save(comment);
    }

    public void removeLikeFromComment(Long commentId, Like like) {
        Comment comment = getComment(commentId);
        comment.getLikes().remove(like);

        log.info("Removing like from comment with ID: {}", comment.getId());
        commentRepository.save(comment);
    }

    public Comment getComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new EntityNotFoundException(COMMENT, commentId));
        log.info("Got comment with ID: {}", commentId);
        return comment;
    }

    public LinkedHashSet<CacheCommentDto> getBatchNewestComments(long postId, int batchSize) {
        LinkedHashSet<Comment> comments =
                new LinkedHashSet<>(commentRepository.findBatchNewestCommentsByPostId(postId, batchSize));
        return comments.stream()
                .map(commentMapper::toCacheCommentDto)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void sendRedisCommentEvent(Comment comment) {
        CommentEventDto commentEventDto = CommentEventDto.builder()
                .postCreatorId(comment.getPost().getAuthorId())
                .commenterId(comment.getAuthorId())
                .postContent(comment.getPost().getContent())
                .commentContent(comment.getContent())
                .build();
        commentMessagePublisher.publish(commentEventDto);
    }
}
