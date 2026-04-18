package com.gigmatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

    @Value("${gigmatch.async.bidder-core-pool-size:20}")
    private int bidderCorePoolSize;

    @Value("${gigmatch.async.bidder-max-pool-size:50}")
    private int bidderMaxPoolSize;

    @Value("${gigmatch.async.bidder-queue-capacity:100}")
    private int bidderQueueCapacity;

    @Value("${gigmatch.async.db-write-core-pool-size:5}")
    private int dbWriteCorePoolSize;

    @Value("${gigmatch.async.db-write-max-pool-size:10}")
    private int dbWriteMaxPoolSize;

    /**
     * Dedicated thread pool for the 5 simulated provider bidder beans.
     * Isolated from the common ForkJoinPool so slow providers (e.g. DataScience: 50-90ms)
     * cannot starve fast ones.
     */
    @Bean(name = "bidderExecutor")
    public Executor bidderExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(bidderCorePoolSize);
        exec.setMaxPoolSize(bidderMaxPoolSize);
        exec.setQueueCapacity(bidderQueueCapacity);
        exec.setThreadNamePrefix("provider-bid-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(10);
        exec.initialize();
        return exec;
    }

    /**
     * Separate thread pool for async fire-and-forget DB writes (match logs, spend updates).
     * These are deliberately not awaited before the response is returned, keeping P99 low.
     */
    @Bean(name = "dbWriteExecutor")
    public Executor dbWriteExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(dbWriteCorePoolSize);
        exec.setMaxPoolSize(dbWriteMaxPoolSize);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("db-write-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(15);
        exec.initialize();
        return exec;
    }
}
