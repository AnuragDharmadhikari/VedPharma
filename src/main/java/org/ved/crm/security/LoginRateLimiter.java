package org.ved.crm.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.ved.crm.common.exception.TooManyRequestsException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final int MAX_ATTEMPTS = 5;

    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private static final String KEY_PREFIX = "login_attempts:";

    // Called BEFORE authentication attempt
    // Throws TooManyRequestsException if limit exceeded
    public void checkRateLimit(String email) {
        String key = KEY_PREFIX + email.toLowerCase();

        String attempts = redisTemplate.opsForValue().get(key);
        int currentAttempts = attempts != null ? Integer.parseInt(attempts) : 0;

        if(currentAttempts >= MAX_ATTEMPTS){
            log.warn("Rate limit exceeded for email: {}", email);
            throw new TooManyRequestsException(
                    "Too many failed login attempts. " +
                            "Please try again after 15 minutes.");
        }
    }

    // Called AFTER failed authentication
    // Increments counter and sets TTL
    public void recordFailedAttempt(String email){
        String key = KEY_PREFIX + email.toLowerCase();

        // Increment counter — if key doesn't exist Redis creates it at 0 then increments
        Long attempts = redisTemplate.opsForValue().increment(key);

        // Set TTL only on first failure — subsequent failures keep original TTL
        // This means the 15 min window starts from the FIRST failed attempt
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, LOCKOUT_DURATION);
        }

        log.warn("Failed login attempt {} of {} for email: {}",
                attempts, MAX_ATTEMPTS, email);
    }

    // Called AFTER successful authentication
    // Clears the counter so legitimate users aren't penalized

    public void clearFailedAttempts(String email) {
        String key = KEY_PREFIX + email.toLowerCase();
        redisTemplate.delete(key);
        log.info("Cleared failed login attempts for email: {}", email);
    }

}
