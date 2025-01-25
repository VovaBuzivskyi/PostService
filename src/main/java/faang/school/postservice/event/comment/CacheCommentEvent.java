package faang.school.postservice.event.comment;

import faang.school.postservice.dto.comment.CacheCommentDto;
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
public class CacheCommentEvent {

    private CacheCommentDto commentDto;
}
