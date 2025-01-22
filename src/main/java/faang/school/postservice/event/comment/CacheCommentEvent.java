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
    private Long id;
    private String content;
    private Long authorId;
    private Long likesCount;
    private Long postId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
