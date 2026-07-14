package com.eduverify.platform.shared.infrastructure.security;

import com.eduverify.platform.shared.domain.model.valueobjects.UserId;

public class CurrentUserContext {
    private static final ThreadLocal<UserId> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(UserId userId) {
        CURRENT_USER.set(userId);
    }

    public static UserId get() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
