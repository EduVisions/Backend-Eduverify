package com.eduverify.platform.shared.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserIdTest {
    @Test
    void generatesRandomValueWhenNoArgConstructorUsed() {
        UserId id = new UserId();
        assertNotNull(id.value());
    }

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
    }

    @Test
    void toStringReturnsRawUuidString() {
        UUID raw = UUID.randomUUID();
        UserId id = new UserId(raw);
        assertEquals(raw.toString(), id.toString());
    }
}
