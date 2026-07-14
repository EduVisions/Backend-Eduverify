package com.eduverify.platform.shared.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentUserContextTest {
    @AfterEach
    void cleanUp() {
        CurrentUserContext.clear();
    }

    @Test
    void returnsNullWhenNothingSet() {
        assertNull(CurrentUserContext.get());
    }

    @Test
    void returnsTheUserIdThatWasSet() {
        UserId userId = new UserId();
        CurrentUserContext.set(userId);
        assertEquals(userId, CurrentUserContext.get());
    }

    @Test
    void clearRemovesTheStoredValue() {
        CurrentUserContext.set(new UserId());
        CurrentUserContext.clear();
        assertNull(CurrentUserContext.get());
    }
}
