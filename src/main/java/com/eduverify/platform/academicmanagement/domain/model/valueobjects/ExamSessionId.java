package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record ExamSessionId(UUID value) {
    public ExamSessionId {
        if (Objects.isNull(value))
            throw new IllegalArgumentException("Exam session identifier cannot be null");
    }

    public ExamSessionId() {
        this(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
