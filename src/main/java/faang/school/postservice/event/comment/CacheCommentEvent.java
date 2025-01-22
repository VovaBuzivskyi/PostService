package faang.school.postservice.event.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheCommentEvent {
    private Long commentId;
    private Long authorId;
    private Long postId;
    private String content;
    private Long likesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
