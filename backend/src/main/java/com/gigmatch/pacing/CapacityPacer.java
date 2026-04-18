package com.gigmatch.pacing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis-backed token bucket capacity pacer for service providers.
 *
 * Each provider has a daily job capacity (e.g. 10 jobs/day). The token bucket
 * refills continuously at a rate of dailyCapacity / 86_400_000 tokens per millisecond.
 * Each matched job deducts 1.0 token. If the bucket is empty, the provider is excluded
 * from the auction until it refills.
 *
 * Redis key: {@code capacity:{providerId}}
 * Hash fields: {@code tokens} (double), {@code lastRefill} (epoch ms as string)
 *
 * All check-and-deduct operations are atomic Lua scripts to prevent race conditions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CapacityPacer {

    private static final String KEY_PREFIX = "capacity:";
    private static final double COST       = 1.0;  // one job slot per match

    private final RedisTemplate<String, String>   redisTemplate;
    private final DefaultRedisScript<Long>         capacityDeductScript;
    private final DefaultRedisScript<String>       capacityReadScript;

    /**
     * Read-only check: does this provider currently have at least one job slot available?
     * Does NOT deduct tokens. Call {@link #deductCapacity} on auction win.
     *
     * @param providerId       the provider to check
     * @param dailyJobCapacity the provider's configured daily capacity (used as maxTokens and refill rate)
     */
    public boolean hasCapacity(long providerId, int dailyJobCapacity) {
        try {
            String key = key(providerId);
            double refillRate = (double) dailyJobCapacity / 86_400_000.0;
            String tokensStr = redisTemplate.execute(capacityReadScript,
                    List.of(key),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(refillRate),
                    String.valueOf(dailyJobCapacity));
            if (tokensStr == null) return true; // key not yet initialised = full capacity
            double tokens = Double.parseDouble(tokensStr);
            return tokens >= COST;
        } catch (Exception e) {
            log.warn("Redis capacity check failed for provider {}: {}", providerId, e.getMessage());
            return true; // fail-open: allow participation if Redis is down
        }
    }

    /**
     * Atomic check-and-deduct: returns true if capacity was available and deducted.
     * Should be called ONLY when a provider wins a match.
     *
     * @param providerId       the winning provider
     * @param dailyJobCapacity the provider's configured daily capacity (used as maxTokens)
     */
    public boolean deductCapacity(long providerId, int dailyJobCapacity) {
        try {
            double refillRate = (double) dailyJobCapacity / 86_400_000.0;
            Long result = redisTemplate.execute(capacityDeductScript,
                    List.of(key(providerId)),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(refillRate),
                    String.valueOf(COST),
                    String.valueOf(dailyJobCapacity));
            return Long.valueOf(1L).equals(result);
        } catch (Exception e) {
            log.warn("Redis capacity deduct failed for provider {}: {}", providerId, e.getMessage());
            return true; // fail-open
        }
    }

    /**
     * Returns the current estimated token count for display on the dashboard.
     */
    public double getRemainingCapacity(long providerId, int dailyJobCapacity) {
        try {
            double refillRate = (double) dailyJobCapacity / 86_400_000.0;
            String result = redisTemplate.execute(capacityReadScript,
                    List.of(key(providerId)),
                    String.valueOf(System.currentTimeMillis()),
                    String.valueOf(refillRate),
                    String.valueOf(dailyJobCapacity));
            return result != null ? Double.parseDouble(result) : dailyJobCapacity;
        } catch (Exception e) {
            log.warn("Redis capacity read failed for provider {}: {}", providerId, e.getMessage());
            return dailyJobCapacity;
        }
    }

    private String key(long providerId) {
        return KEY_PREFIX + providerId;
    }

}
