package faang.school.postservice.model.cache;

import faang.school.postservice.dto.comment.CommentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCacheDto implements Serializable {

    private Long postId;
    private String content;
    private Long authorId;
    private Long projectId;
    private long likesCount;

    @Builder.Default
    private Set<CommentDto> comments = new LinkedHashSet<>();   // put by hands, max length = 3 + length get from props
    private long commentsCount;
    private long postViewsCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    public void incrementLikesCount() {
        likesCount++;
    }
}
