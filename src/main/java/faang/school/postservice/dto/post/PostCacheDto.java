package faang.school.postservice.dto.post;

import faang.school.postservice.dto.comment.CommentDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostCacheDto {

    private Long id;
    private String content;
    private Long authorId;
    private Long projectId;
    private long likesCount;
    private Set<CommentDto> comments = new LinkedHashSet<>();   // put by hands
    private long commentsCount;
    private LocalDateTime publishedAt;
    private LocalDateTime updatedAt;
}
