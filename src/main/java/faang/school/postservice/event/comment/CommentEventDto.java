package faang.school.postservice.event.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class CommentEventDto {

    @NotNull
    private Long postCreatorId;

    @NotNull
    private Long commenterId;

    @NotNull
    @NotBlank
    private String postContent;

    @NotNull
    @NotBlank
    private String commentContent;
}