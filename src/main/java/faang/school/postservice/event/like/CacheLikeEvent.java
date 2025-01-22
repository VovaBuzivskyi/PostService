package faang.school.postservice.event.like;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheLikeEvent {
    private Long likeId;
    private Long likeAuthorId;
    private Long commentId;
    private Long postId;
}
