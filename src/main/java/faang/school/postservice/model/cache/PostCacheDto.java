package faang.school.postservice.model.cache;

import faang.school.postservice.dto.comment.CacheCommentDto;
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

    @Builder.Default
    private Set<CacheCommentDto> comments = new LinkedHashSet<>();
    private Long postId;
    private String content;
    private Long authorId;
    private Long projectId;
    private long likesCount;
    private long commentsCount;
    private long postViewsCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;

    public void incrementLikesCount() {
        likesCount++;
    }

    public void incrementPostViewsCount() {
        postViewsCount++;
    }

    public void incrementCommentsCount() {
        commentsCount++;
    }
}
