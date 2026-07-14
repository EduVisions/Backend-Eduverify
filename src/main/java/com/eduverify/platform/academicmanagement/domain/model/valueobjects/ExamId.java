package com.eduverify.platform.academicmanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

public record ExamId(UUID value) {
    public ExamId {
        if (Objects.isNull(value))
            throw new IllegalArgumentException("Exam identifier cannot be null");
    }

    public ExamId() {
        this(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
