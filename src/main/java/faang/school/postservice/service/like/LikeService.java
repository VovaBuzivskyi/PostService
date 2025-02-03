package faang.school.postservice.service.like;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.like.LikeDto;
import faang.school.postservice.exception.EntityNotFoundException;
import faang.school.postservice.mapper.like.LikeMapper;
import faang.school.postservice.model.Comment;
import faang.school.postservice.model.Like;
import faang.school.postservice.model.Post;
import faang.school.postservice.publisher.kafka.KafkaAddLikeProducer;
import faang.school.postservice.repository.LikeRepository;
import faang.school.postservice.service.comment.CommentService;
import faang.school.postservice.service.post.PostService;
import faang.school.postservice.validator.like.LikeValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikeService {

    private static final String LIKE = "Like";

    private final LikeRepository likeRepository;
    private final UserServiceClient userServiceClient;
    private final PostService postService;
    private final CommentService commentService;
    private final LikeValidator likeValidator;
    private final LikeMapper likeMapper;
    private final KafkaAddLikeProducer kafkaAddLikeProducer;

    @Transactional
    public void addLikeToPost(LikeDto likeDto) {
        validateUserExistence(likeDto.getUserId());
        likeValidator.validateLikeHasTarget(likeDto.getPostId(), likeDto.getCommentId());

        List<Like> likesOfPost = likeRepository.findByPostId(likeDto.getPostId());
        likeValidator.validateUserAddOnlyOneLikeToPost(likesOfPost, likeDto.getUserId());

        Like likeToCheckComment = getLikeOrEmptyLike(likeDto.getId());
        likeValidator.validateLikeWasNotPutToComment(likeDto, likeToCheckComment);

        Post postOfLike = postService.getPost(likeDto.getPostId());
        Like likeToSave = likeMapper.toEntity(likeDto);
        likeToSave.setPost(postOfLike);

        Like savedLike = likeRepository.save(likeToSave);
        postService.addLikeToPost(likeDto.getPostId(), savedLike);

        kafkaAddLikeProducer.send(String.valueOf(savedLike.getPost().getId()));
        log.info("Save new Like for Post with ID: {}", likeDto.getPostId());
    }

    @Transactional
    public void addLikeToComment(LikeDto likeDto) {
        validateUserExistence(likeDto.getUserId());
        likeValidator.validateLikeHasTarget(likeDto.getCommentId(), likeDto.getPostId());

        List<Like> likesOfComment = likeRepository.findByCommentId(likeDto.getCommentId());
        likeValidator.validateUserAddOnlyOneLikeToComment(likesOfComment, likeDto.getUserId());

        Like likeToCheckPost = getLikeOrEmptyLike(likeDto.getId());
        likeValidator.validateLikeWasNotPutToPost(likeDto, likeToCheckPost);

        Comment commentOfLike = commentService.getComment(likeDto.getCommentId());
        Like likeToSave = likeMapper.toEntity(likeDto);
        likeToSave.setComment(commentOfLike);

        Like saveLike = likeRepository.save(likeToSave);
        commentService.addLikeToComment(likeDto.getCommentId(), saveLike);
        log.info("Save new Like for Comment with ID: {}", likeDto.getCommentId());
    }

    public void removeLikeFromPost(Long likeId, LikeDto likeDto) {
        Like likeToRemove = removeLike(likeId, likeDto);
        postService.removeLikeFromPost(likeDto.getPostId(), likeToRemove);
    }

    public void removeLikeFromComment(Long likeId, LikeDto likeDto) {
        Like likeToRemove = removeLike(likeId, likeDto);
        commentService.removeLikeFromComment(likeDto.getCommentId(), likeToRemove);
    }

    private Like removeLike(Long likeId, LikeDto likeDto) {
        validateUserExistence(likeDto.getUserId());
        Like likeToRemove = getLike(likeId);
        likeValidator.validateThisUserAddThisLike(likeDto.getUserId(), likeToRemove);
        likeRepository.delete(likeToRemove);
        log.info("Remove like with ID: {}", likeId);
        return likeToRemove;
    }

    private void validateUserExistence(long id) {
        userServiceClient.getUser(id);
    }

    private Like getLikeOrEmptyLike(Long likeId) {
        return likeRepository.findById(likeId).orElse(new Like());
    }

    private Like getLike(long likeId) {
        return likeRepository.findById(likeId).orElseThrow(() ->
                new EntityNotFoundException(LIKE, likeId));
    }
}
