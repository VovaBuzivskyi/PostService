package faang.school.postservice.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(value = "task-executor")
public class ThreadPoolProperties {

    private ExecutorProperties fileUpload;
    private ExecutorProperties postTask;
    private ExecutorProperties newsFeedTask;

    @Data
    public static class ExecutorProperties {
        private int corePoolSize;
        private int maxPoolSize;
        private int queueCapacity;
    }
}
