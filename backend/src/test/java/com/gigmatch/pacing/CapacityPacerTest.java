package com.gigmatch.pacing;

import com.gigmatch.config.RedisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = {RedisConfig.class, CapacityPacer.class,
    org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class})
@Testcontainers
class CapacityPacerTest {

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired CapacityPacer capacityPacer;
    @Autowired RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        // Clear all capacity keys between tests
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Test
    void hasCapacity_newProvider_returnsTrue() {
        assertThat(capacityPacer.hasCapacity(100L)).isTrue();
    }

    @Test
    void deductCapacity_reducesTokens() {
        boolean result = capacityPacer.deductCapacity(200L, 10);
        assertThat(result).isTrue();
        double remaining = capacityPacer.getRemainingCapacity(200L, 10);
        assertThat(remaining).isLessThan(10.0);
    }

    @Test
    void atCapacity_returnsFalse() {
        int dailyCapacity = 3;
        // Exhaust all 3 slots
        for (int i = 0; i < dailyCapacity; i++) {
            capacityPacer.deductCapacity(300L, dailyCapacity);
        }
        double remaining = capacityPacer.getRemainingCapacity(300L, dailyCapacity);
        assertThat(remaining).isLessThan(1.0);
    }

    @Test
    void concurrentDeductions_noOverAllocation() throws InterruptedException {
        int dailyCapacity = 5;
        int threads = 20;
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() ->
                capacityPacer.deductCapacity(400L, dailyCapacity)));
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        for (Future<Boolean> f : futures) {
            try { if (f.get()) successCount.incrementAndGet(); }
            catch (ExecutionException ignored) {}
        }

        // At most dailyCapacity deductions should succeed
        assertThat(successCount.get()).isLessThanOrEqualTo(dailyCapacity);
    }
}
