package faang.school.postservice.event.post;

import faang.school.postservice.model.cache.PostCacheDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublishPostEvent {
    private PostCacheDto postDto;
    private List<Long> followersIds;
}
