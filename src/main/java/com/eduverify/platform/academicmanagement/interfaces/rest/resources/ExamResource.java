package com.eduverify.platform.academicmanagement.interfaces.rest.resources;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;

import java.time.LocalDateTime;

public record ExamResource(String id, String teacherId, String title, LocalDateTime scheduledDate,
                            int durationMinutes, String status, int sessionCount) {
    public static ExamResource from(Exam exam) {
        return new ExamResource(
                exam.getId().toString(),
                exam.getTeacherId().toString(),
                exam.getTitle(),
                exam.getScheduledDate(),
                exam.getDurationMinutes(),
                exam.getStatus().name(),
                exam.getSessions().size()
        );
    }
}
