package com.eduverify.platform.iam.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenStoreTest {
    private final TokenStore tokenStore = new TokenStore();

    @Test
    void issuedTokenResolvesToTheSameUser() {
        UserId userId = new UserId();
        String token = tokenStore.issue(userId);

        assertEquals(userId, tokenStore.resolve(token).orElseThrow());
    }

    @Test
    void unknownTokenResolvesToEmpty() {
        assertTrue(tokenStore.resolve("unknown-token").isEmpty());
    }

    @Test
    void revokedTokenNoLongerResolves() {
        UserId userId = new UserId();
        String token = tokenStore.issue(userId);

        tokenStore.revoke(token);

        assertTrue(tokenStore.resolve(token).isEmpty());
    }
}
