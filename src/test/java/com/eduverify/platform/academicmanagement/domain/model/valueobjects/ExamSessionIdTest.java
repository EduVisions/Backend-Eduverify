package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamSessionIdTest {
    @Test
    void generatesRandomValueWhenNoArgConstructorUsed() {
        assertNotNull(new ExamSessionId().value());
    }

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new ExamSessionId(null));
    }

    @Test
    void toStringReturnsRawUuidString() {
        UUID raw = UUID.randomUUID();
        assertEquals(raw.toString(), new ExamSessionId(raw).toString());
    }
}
