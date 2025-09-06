package com.tbm.admin.config;

import com.tbm.admin.exception.AsyncServiceException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @Async 활성화, 비동기 로직을 별도로 실행하기 위한 쓰레드 풀 설정.
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {
    
    private static final int CORE_POOL_SIZE = 16;
    
    public static final String TELEGRAM_EXECUTOR = "telegramExecutor";

    @Bean(name= TELEGRAM_EXECUTOR)
    public TaskExecutor taskExecutor() {
        return generateThreadPoolTaskExecutor();
    }
    
    private TaskExecutor generateThreadPoolTaskExecutor() {
        
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(CORE_POOL_SIZE);
        taskExecutor.setRejectedExecutionHandler(new AsyncServiceException());
        taskExecutor.afterPropertiesSet();
        taskExecutor.initialize();
        return taskExecutor;
    }
}