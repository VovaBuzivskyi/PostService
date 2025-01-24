package faang.school.postservice.dto.feed;

import faang.school.postservice.dto.post.PostCacheDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedCacheDto implements Serializable {

    private long userId;
    private Set<PostCacheDto> posts = new LinkedHashSet<>();
}
