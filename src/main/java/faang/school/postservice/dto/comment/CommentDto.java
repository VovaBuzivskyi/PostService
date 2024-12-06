package faang.school.postservice.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long id;

    @NotBlank(message = "content must be not blank")
    @Size(max = 4096, message = "content must be max at 4096 characters")
    private String content;

    @NotNull
    private Long authorId;

    @NotNull
    private Long postId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> likesId;

    @NotNull(message = "verify status can't be empty")
    private Boolean verified;
}
