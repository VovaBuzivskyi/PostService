package faang.school.postservice.validator.comment;

import faang.school.postservice.client.UserServiceClient;
import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CommentValidatorTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentValidator commentValidator;

    @Test
    public void isAuthorExistNotFound() {
        Long authorId = 1L;
        when(userServiceClient.getUser(authorId)).thenReturn(null);
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class,
                () -> commentValidator.isAuthorExist(authorId));
        assertEquals("Author with ID " + authorId + " does not exist", ex.getMessage());
        verify(userServiceClient, times(1)).getUser(authorId);
    }

    @Test
    public void isAuthorExistTest() {
        Long authorId = 1L;
        UserDto userDto = new UserDto(authorId, "test", "test", false);
        when(userServiceClient.getUser(authorId)).thenReturn(userDto);
        commentValidator.isAuthorExist(authorId);
        verify(userServiceClient, times(1)).getUser(authorId);
    }

    @Test
    public void validateCommentExistsTest() {
        long commentId = 1L;

        when(commentRepository.existsById(commentId)).thenReturn(true);

        assertDoesNotThrow(() -> commentValidator.validateCommentExists(commentId));
    }

    @Test
    public void validateCommentExistsThrowsExceptionTest() {
        long commentId = 1L;

        when(commentRepository.existsById(commentId)).thenReturn(false);

        assertThrows(EntityNotFoundException.class,
                () -> commentValidator.validateCommentExists(commentId));
    }
}
