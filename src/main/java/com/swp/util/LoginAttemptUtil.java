package com.swp.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LoginAttemptUtil {

    private static final int MAX_ATTEMPT = 5;
    private static final int LOCK_TIME_DURATION = 30; // minutes

    private static final Map<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> lockCache = new ConcurrentHashMap<>();

    public static void loginSucceeded(String key) {
        if (key == null) return;
        attemptsCache.remove(key);
        lockCache.remove(key);
    }

    public static void loginFailed(String key) {
        if (key == null) return;
        int attempts = attemptsCache.getOrDefault(key, 0);
        attempts++;
        attemptsCache.put(key, attempts);
        if (attempts >= MAX_ATTEMPT) {
            lockCache.put(key, LocalDateTime.now().plusMinutes(LOCK_TIME_DURATION));
        }
    }

    public static boolean isLocked(String key) {
        if (key == null) return false;
        if (lockCache.containsKey(key)) {
            LocalDateTime lockTime = lockCache.get(key);
            if (lockTime.isAfter(LocalDateTime.now())) {
                return true;
            } else {
                // Lock expired
                lockCache.remove(key);
                attemptsCache.remove(key);
                return false;
            }
        }
        return false;
    }

    public static long getRemainingLockTimeInMinutes(String key) {
        if (key == null) return 0;
        if (lockCache.containsKey(key)) {
            LocalDateTime lockTime = lockCache.get(key);
            long diff = Duration.between(LocalDateTime.now(), lockTime).toMinutes();
            return diff > 0 ? diff : 1; // At least 1 min if still locked
        }
        return 0;
    }
}
