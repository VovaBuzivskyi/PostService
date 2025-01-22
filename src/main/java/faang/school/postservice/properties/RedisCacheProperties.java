package faang.school.postservice.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.data.cache")
public class RedisCacheProperties {

    private int globalMinutesTtl;
    private int feedsHoursTtl;
    private String feedsCacheName;
    private int postsHoursTtl;
    private String postsCacheName;
    private int usersHoursTtl;
    private String usersCacheName;
}
