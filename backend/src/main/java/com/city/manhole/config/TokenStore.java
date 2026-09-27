package com.city.manhole.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 简单内存令牌存储（滑动过期 12 小时） */
@Component
public class TokenStore {
    private static final long TTL_SECONDS = 12 * 3600;

    private record TokenInfo(Long userId, Instant expireAt) {}

    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();

    public String create(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new TokenInfo(userId, Instant.now().plusSeconds(TTL_SECONDS)));
        return token;
    }

    public Long getUserId(String token) {
        if (token == null) return null;
        TokenInfo info = tokens.get(token);
        if (info == null) return null;
        if (info.expireAt().isBefore(Instant.now())) {
            tokens.remove(token);
            return null;
        }
        // 滑动续期
        tokens.put(token, new TokenInfo(info.userId(), Instant.now().plusSeconds(TTL_SECONDS)));
        return info.userId();
    }

    public void remove(String token) {
        tokens.remove(token);
    }
}
