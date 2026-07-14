package com.eduverify.platform.academicmanagement.domain.services;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamService {
    Exam createExam(UserId teacherId, String title, LocalDateTime scheduledDate, int durationMinutes);
    List<Exam> listExams();
    Exam getExam(ExamId examId);
    Exam startExam(ExamId examId);
    Exam finishExam(ExamId examId);
    ExamSession accessExam(ExamId examId, UserId studentId);
    Exam submitExam(ExamId examId, ExamSessionId sessionId);
}
