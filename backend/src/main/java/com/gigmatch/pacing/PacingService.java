package com.gigmatch.pacing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Handles deduplication: prevents the same provider from being matched to the
 * same client repeatedly within a 24-hour window.
 *
 * Redis key: {@code seen:{clientId}:{providerId}} — TTL 24 hours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PacingService {

    private static final String KEY_PREFIX = "seen:";
    private static final Duration TTL      = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Returns true if this client–provider pair has been seen recently (within TTL).
     * Idempotent: calling this again within TTL returns true without resetting the TTL.
     */
    public boolean isRecentMatch(String clientId, long providerId) {
        try {
            String key = KEY_PREFIX + clientId + ":" + providerId;
            // setIfAbsent returns TRUE if the key was new (i.e., NOT a recent match)
            Boolean isNew = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", TTL);
            return !Boolean.TRUE.equals(isNew);
        } catch (Exception e) {
            log.warn("Redis dedup check failed for client={} provider={}: {}",
                    clientId, providerId, e.getMessage());
            return false; // fail-open: allow participation if Redis is down
        }
    }
}
