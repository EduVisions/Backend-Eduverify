package com.eduverify.platform.academicmanagement.interfaces.rest.resources;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;

public record ExamSessionResource(String id, String examId, String studentId, String status) {
    public static ExamSessionResource from(ExamSession session) {
        return new ExamSessionResource(
                session.getId().toString(),
                session.getExamId().toString(),
                session.getStudentId().toString(),
                session.getStatus().name()
        );
    }
}
