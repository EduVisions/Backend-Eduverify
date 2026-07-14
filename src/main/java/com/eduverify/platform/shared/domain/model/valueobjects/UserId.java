package com.eduverify.platform.shared.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        if (Objects.isNull(value))
            throw new IllegalArgumentException("User identifier cannot be null");
    }

    public UserId() {
        this(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
