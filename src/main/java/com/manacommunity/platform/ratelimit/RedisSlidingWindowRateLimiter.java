package com.manacommunity.platform.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Shared atomic Redis + Lua sliding-window rate limiter.
 *
 * <p>Stores request timestamps in a sorted set ({@code ZSET}) and runs a single Lua script
 * per check: prune expired members, count active entries, and add + extend TTL if under limit.</p>
 *
 * <p>Returns {@code null} when Redis is unavailable or throws an error so callers can
 * transparently fall back to their in-memory sliding window.</p>
 */
@Slf4j
@Component
public class RedisSlidingWindowRateLimiter {

    private static final String LUA_SCRIPT = """
            local key = KEYS[1]
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            local member = ARGV[4]
            redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
            local count = redis.call('ZCARD', key)
            if count >= limit then
                return 0
            end
            redis.call('ZADD', key, now, member)
            redis.call('PEXPIRE', key, window + 5000)
            return 1
            """;

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT;

    static {
        SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>();
        SLIDING_WINDOW_SCRIPT.setScriptText(LUA_SCRIPT);
        SLIDING_WINDOW_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public RedisSlidingWindowRateLimiter(@Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Attempts to acquire one slot in the sliding window.
     *
     * @param key      Redis ZSET key
     * @param windowMs sliding window duration in milliseconds
     * @param limit    maximum allowed requests inside the window
     * @return {@code Boolean.TRUE} if allowed, {@code Boolean.FALSE} if limit exceeded,
     *         or {@code null} if Redis is unavailable/errored (triggering local fallback)
     */
    public Boolean tryAcquire(String key, long windowMs, int limit) {
        if (redisTemplate == null || key == null) {
            return null;
        }
        try {
            long nowMs = System.currentTimeMillis();
            String member = nowMs + ":" + UUID.randomUUID();
            Long result = redisTemplate.execute(
                    SLIDING_WINDOW_SCRIPT,
                    List.of(key),
                    String.valueOf(nowMs),
                    String.valueOf(windowMs),
                    String.valueOf(limit),
                    member
            );
            if (result == null) {
                return null;
            }
            return result == 1L;
        } catch (Exception e) {
            log.warn("Redis Lua sliding-window failed for key={}, falling back to in-memory: {}", key, e.getMessage());
            return null;
        }
    }
}
