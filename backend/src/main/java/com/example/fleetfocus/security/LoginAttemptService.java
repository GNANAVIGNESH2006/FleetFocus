package com.example.fleetfocus.security;

import com.example.fleetfocus.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_DURATION_SECONDS = 15 * 60; // 15 minutes

    private static class AttemptRecord {
        int failedCount;
        Instant lockedUntil;
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        AttemptRecord record = attempts.get(key);
        if (record == null) {
            return;
        }
        synchronized (record) {
            if (record.lockedUntil != null) {
                if (Instant.now().isBefore(record.lockedUntil)) {
                    throw new TooManyRequestsException(
                            "Too many failed login attempts. Please try again in 15 minutes.");
                } else {
                    record.failedCount = 0;
                    record.lockedUntil = null;
                }
            }
        }
    }

    public void recordFailure(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        AttemptRecord record = attempts.computeIfAbsent(key, k -> new AttemptRecord());
        synchronized (record) {
            if (record.lockedUntil != null && !Instant.now().isBefore(record.lockedUntil)) {
                record.failedCount = 0;
                record.lockedUntil = null;
            }
            record.failedCount++;
            if (record.failedCount >= MAX_ATTEMPTS) {
                record.lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_SECONDS);
            }
        }
    }

    public void recordSuccess(String username, String clientIp) {
        String key = buildKey(username, clientIp);
        attempts.remove(key);
    }

    public void clearAll() {
        attempts.clear();
    }

    private String buildKey(String username, String clientIp) {
        String normUser = username != null ? username.trim().toLowerCase() : "";
        String normIp = (clientIp != null && !clientIp.trim().isEmpty()) ? clientIp.trim() : "unknown";
        return normUser + "|" + normIp;
    }
}
