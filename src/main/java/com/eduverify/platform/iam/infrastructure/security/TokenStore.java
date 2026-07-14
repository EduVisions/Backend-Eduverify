package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TokenStore {
    private final Map<String, UserId> tokensToUsers = new ConcurrentHashMap<>();

    public String issue(UserId userId) {
        String token = UUID.randomUUID().toString();
        tokensToUsers.put(token, userId);
        return token;
    }

    public Optional<UserId> resolve(String token) {
        return Optional.ofNullable(tokensToUsers.get(token));
    }

    public void revoke(String token) {
        tokensToUsers.remove(token);
    }
}
