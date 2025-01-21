package faang.school.postservice.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.kafka.topics")
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

}
