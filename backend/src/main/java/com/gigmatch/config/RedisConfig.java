package com.gigmatch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Lua script for atomic token bucket check-and-deduct.
     * KEYS[1] = capacity:{providerId}
     * ARGV[1] = now (epoch ms as string)
     * ARGV[2] = refillRatePerMs  (dailyCapacity / 86_400_000.0)
     * ARGV[3] = cost             (always "1" — one job slot)
     * ARGV[4] = maxTokens        (dailyJobCapacity)
     * Returns: "1" = success (capacity available and deducted), "0" = insufficient capacity
     */
    @Bean(name = "capacityDeductScript")
    public DefaultRedisScript<Long> capacityDeductScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
            "local key = KEYS[1] " +
            "local now = tonumber(ARGV[1]) " +
            "local refillRate = tonumber(ARGV[2]) " +
            "local cost = tonumber(ARGV[3]) " +
            "local maxTokens = tonumber(ARGV[4]) " +
            "local data = redis.call('HMGET', key, 'tokens', 'lastRefill') " +
            "local tokens = tonumber(data[1]) " +
            "local lastRefill = tonumber(data[2]) " +
            "if tokens == nil then tokens = maxTokens end " +
            "if lastRefill == nil then lastRefill = now end " +
            "local elapsed = math.max(0, now - lastRefill) " +
            "tokens = math.min(maxTokens, tokens + elapsed * refillRate) " +
            "if tokens < cost then " +
            "  redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now) " +
            "  redis.call('EXPIRE', key, 86400) " +
            "  return 0 " +
            "end " +
            "tokens = tokens - cost " +
            "redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', now) " +
            "redis.call('EXPIRE', key, 86400) " +
            "return 1"
        );
        return script;
    }

    /**
     * Read-only Lua script that returns current token count without deducting.
     * Used for capacity checks before entering the auction.
     */
    @Bean(name = "capacityReadScript")
    public DefaultRedisScript<String> capacityReadScript() {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(
            "local key = KEYS[1] " +
            "local now = tonumber(ARGV[1]) " +
            "local refillRate = tonumber(ARGV[2]) " +
            "local maxTokens = tonumber(ARGV[3]) " +
            "local data = redis.call('HMGET', key, 'tokens', 'lastRefill') " +
            "local tokens = tonumber(data[1]) " +
            "local lastRefill = tonumber(data[2]) " +
            "if tokens == nil then return tostring(maxTokens) end " +
            "if lastRefill == nil then return tostring(tokens) end " +
            "local elapsed = math.max(0, now - lastRefill) " +
            "tokens = math.min(maxTokens, tokens + elapsed * refillRate) " +
            "return tostring(tokens)"
        );
        return script;
    }
}
