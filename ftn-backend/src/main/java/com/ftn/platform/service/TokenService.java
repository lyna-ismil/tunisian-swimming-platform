package com.ftn.platform.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {

    private final Map<String, SessionInfo> tokenMap = new ConcurrentHashMap<>();

    @Value("${auth.token-expiration-minutes:60}")
    private int expirationMinutes;

    public String createToken(String username, String role) {
        String token = UUID.randomUUID().toString();
        tokenMap.put(token, new SessionInfo(username, role, LocalDateTime.now().plusMinutes(expirationMinutes)));
        return token;
    }

    public boolean validateToken(String token) {
        if (token == null) return false;
        SessionInfo info = tokenMap.get(token);
        if (info == null) return false;
        if (info.getExpiry().isBefore(LocalDateTime.now())) {
            tokenMap.remove(token);
            return false;
        }
        // Extend session on activity
        info.setExpiry(LocalDateTime.now().plusMinutes(expirationMinutes));
        return true;
    }

    public SessionInfo getSession(String token) {
        return tokenMap.get(token);
    }

    public void removeToken(String token) {
        if (token != null) {
            tokenMap.remove(token);
        }
    }

    @Data
    @AllArgsConstructor
    public static class SessionInfo {
        private String username;
        private String role;
        private LocalDateTime expiry;
    }
}
