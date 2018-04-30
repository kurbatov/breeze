package com.github.kurbatov.breeze.conf;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Configuration
@EnableAsync
@EnableScheduling
@ComponentScan(
        basePackages = "com.github.kurbatov.breeze",
        excludeFilters = @ComponentScan.Filter(pattern = "com\\.github\\.kurbatov\\.breeze\\.(conf)\\..*", type = FilterType.REGEX)
)
@Import({
    ResourcesConfiguration.class,
    //SecurityConfiguration.class,
    ServerConfiguration.class,
    CassandraConfiguration.class
})
public class ApplicationContextConfiguration implements AsyncConfigurer {
    
    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
    
    @Override
    @Bean(name = "taskExecutor", initMethod = "initialize", destroyMethod = "shutdown")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(16);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("TaskExecutor-");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService taskScheduler() {
        return Executors.newScheduledThreadPool(8);
    }

}
