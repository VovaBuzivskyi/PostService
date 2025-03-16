package faang.school.postservice.validator.comment;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CommentValidator {
    private final UserServiceClient userServiceClient;
    private final CommentRepository commentRepository;

    public void isAuthorExist(Long authorId) {
        log.info("Checking if author exists with ID: {}", authorId);
        if (userServiceClient.getUser(authorId) == null) {
            log.error("Author with ID: {} does not exist", authorId);
            throw new EntityNotFoundException("Author with ID " + authorId + " does not exist");
        }
    }

    public void isCommentExists(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new EntityNotFoundException("Comment with ID " + commentId + " does not exist");
        }
    }
}
