package faang.school.postservice.model.cache;

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

    @Builder.Default
    private Set<PostCacheDto> posts = new LinkedHashSet<>();
}
