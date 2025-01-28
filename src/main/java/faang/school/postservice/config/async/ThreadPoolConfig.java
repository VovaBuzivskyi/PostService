package faang.school.postservice.config.async;

import faang.school.postservice.properties.ThreadPoolProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@RequiredArgsConstructor
public class ThreadPoolConfig {

    private final ThreadPoolProperties props;

    @Bean(name = "fileUploadTaskExecutor")
    public ThreadPoolTaskExecutor fileUploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getFileUpload().getCorePoolSize());
        executor.setMaxPoolSize(props.getFileUpload().getMaxPoolSize());
        executor.setQueueCapacity(props.getFileUpload().getQueueCapacity());
        executor.setThreadNamePrefix("FileUploadAsync-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor postTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getPostTask().getCorePoolSize());
        executor.setMaxPoolSize(props.getPostTask().getMaxPoolSize());
        executor.setQueueCapacity(props.getPostTask().getQueueCapacity());
        executor.setThreadNamePrefix("publish-posts-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor newsFeedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(props.getNewsFeedTask().getCorePoolSize());
        executor.setMaxPoolSize(props.getNewsFeedTask().getMaxPoolSize());
        executor.setQueueCapacity(props.getNewsFeedTask().getQueueCapacity());
        executor.setThreadNamePrefix("publish-posts-");
        executor.initialize();
        return executor;
    }
}
