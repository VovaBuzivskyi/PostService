package faang.school.postservice.dto.news_feed;

import faang.school.postservice.model.cache.PostCacheDto;
import faang.school.postservice.model.cache.UserCacheDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsFeedResponseDto {

    private List<UserCacheDto> postsAuthors;
    private Set<PostCacheDto> posts = new LinkedHashSet<>();
}
