package faang.school.postservice.config.async;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ThreadPoolConfig {

    @Value("${task-executor.file-upload.core-pool-size}")
    private int corePoolSize;

    @Value("${task-executor.file-upload.max-pool-size}")
    private int maxPoolSize;

    @Value("${task-executor.file-upload.queue-capacity}")
    private int queueCapacity;

    @Bean(name = "fileUploadTaskExecutor")
    public ThreadPoolTaskExecutor fileUploadTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("FileUploadAsync-");
        executor.initialize();
        return executor;
    }

    // take values from properties
    @Bean
    public Executor postTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("publish-posts-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor newsFeedTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("publish-posts-");
        executor.initialize();
        return executor;
    }
}
