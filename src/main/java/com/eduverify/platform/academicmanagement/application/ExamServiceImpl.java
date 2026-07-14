package com.eduverify.platform.academicmanagement.application;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.services.ExamRepository;
import com.eduverify.platform.academicmanagement.domain.services.ExamService;
import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamServiceImpl implements ExamService {
    private final ExamRepository examRepository;

    public ExamServiceImpl(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Override
    public Exam createExam(UserId teacherId, String title, LocalDateTime scheduledDate, int durationMinutes) {
        Exam exam = new Exam(teacherId, title, scheduledDate, durationMinutes);
        return examRepository.save(exam);
    }

    @Override
    public List<Exam> listExams() {
        return examRepository.findAll();
    }

    @Override
    public Exam getExam(ExamId examId) {
        return findExamOrThrow(examId);
    }

    @Override
    public Exam startExam(ExamId examId) {
        Exam exam = findExamOrThrow(examId);
        exam.start();
        return examRepository.save(exam);
    }

    @Override
    public Exam finishExam(ExamId examId) {
        Exam exam = findExamOrThrow(examId);
        exam.finish();
        return examRepository.save(exam);
    }

    @Override
    public ExamSession accessExam(ExamId examId, UserId studentId) {
        Exam exam = findExamOrThrow(examId);
        ExamSession session = exam.startSessionFor(studentId);
        examRepository.save(exam);
        return session;
    }

    @Override
    public Exam submitExam(ExamId examId, ExamSessionId sessionId) {
        Exam exam = findExamOrThrow(examId);
        exam.finishSession(sessionId);
        return examRepository.save(exam);
    }

    private Exam findExamOrThrow(ExamId examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> new NotFoundException("No exam found with id: " + examId));
    }
}
