package faang.school.postservice.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "application.kafka.topics")
public class KafkaTopicsProperties {

    private String postTopicName;
    private int postPartitionsCount;
    private short postReplicationsCount;

    private String likeTopicName;
    private int likePartitionsCount;
    private short likeReplicationsCount;

    private String commentTopicName;
    private int commentPartitionsCount;
    private short commentReplicationsCount;

    private String cacheUserTopicName;
    private int cacheUserPartitionsCount;
    private short cacheUserReplicationsCount;

    private String postViewsTopicName;
    private int postViewsPartitionsCount;
    private short postViewsReplicationsCount;

    private String heatCacheTopicName;
    private int heatCachePartitionsCount;
    private short heatCacheReplicationsCount;

    private String heatFeedCacheTopicName;
    private int heatFeedCachePartitionsCount;
    private short heatFeedCacheReplicationsCount;
}
