package com.eduverify.platform.academicmanagement.application;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamStatus;
import com.eduverify.platform.academicmanagement.infrastructure.persistence.InMemoryExamRepository;
import com.eduverify.platform.shared.domain.exceptions.NotFoundException;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExamServiceImplTest {
    private ExamServiceImpl service;
    private final UserId teacherId = new UserId();

    @BeforeEach
    void setUp() {
        service = new ExamServiceImpl(new InMemoryExamRepository());
    }

    @Test
    void createExamPersistsAndReturnsIt() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);

        assertEquals(exam.getId(), service.getExam(exam.getId()).getId());
    }

    @Test
    void listExamsReturnsAllCreatedExams() {
        service.createExam(teacherId, "Quiz 1", LocalDateTime.now().plusDays(1), 45);
        service.createExam(teacherId, "Quiz 2", LocalDateTime.now().plusDays(2), 60);

        assertEquals(2, service.listExams().size());
    }

    @Test
    void getExamThrowsNotFoundForUnknownId() {
        assertThrows(NotFoundException.class, () -> service.getExam(new ExamId()));
    }

    @Test
    void startExamMovesItToInProgress() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);

        Exam started = service.startExam(exam.getId());

        assertEquals(ExamStatus.IN_PROGRESS, started.getStatus());
    }

    @Test
    void finishExamMovesItToFinished() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);
        service.startExam(exam.getId());

        Exam finished = service.finishExam(exam.getId());

        assertEquals(ExamStatus.FINISHED, finished.getStatus());
    }

    @Test
    void accessExamCreatesASessionForTheStudent() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);
        UserId studentId = new UserId();

        ExamSession session = service.accessExam(exam.getId(), studentId);

        assertEquals(studentId, session.getStudentId());
        assertEquals(1, service.getExam(exam.getId()).getSessions().size());
    }

    @Test
    void submitExamFinishesTheMatchingSession() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);
        ExamSession session = service.accessExam(exam.getId(), new UserId());

        service.submitExam(exam.getId(), session.getId());

        assertEquals(
                com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionStatus.FINISHED,
                service.getExam(exam.getId()).getSessions().get(0).getStatus()
        );
    }

    @Test
    void submitExamThrowsForUnknownSession() {
        Exam exam = service.createExam(teacherId, "Quiz", LocalDateTime.now().plusDays(1), 45);

        assertThrows(IllegalArgumentException.class,
                () -> service.submitExam(exam.getId(), new ExamSessionId()));
    }
}
