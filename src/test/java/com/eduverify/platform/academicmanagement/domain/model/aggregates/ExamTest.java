package com.eduverify.platform.academicmanagement.domain.model.aggregates;

import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionStatus;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamStatus;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamTest {
    private final UserId teacherId = new UserId();

    private Exam newExam() {
        return new Exam(teacherId, "Calculo Diferencial - Parcial 2", LocalDateTime.now().plusDays(1), 90);
    }

    @Test
    void createsExamInScheduledStatus() {
        Exam exam = newExam();

        assertNotNull(exam.getId());
        assertEquals(ExamStatus.SCHEDULED, exam.getStatus());
        assertTrue(exam.getSessions().isEmpty());
    }

    @Test
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new Exam(teacherId, "  ", LocalDateTime.now(), 90));
    }

    @Test
    void rejectsNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> new Exam(teacherId, "Quiz", LocalDateTime.now(), 0));
    }

    @Test
    void startMovesFromScheduledToInProgress() {
        Exam exam = newExam();
        exam.start();
        assertEquals(ExamStatus.IN_PROGRESS, exam.getStatus());
    }

    @Test
    void startFailsWhenNotScheduled() {
        Exam exam = newExam();
        exam.start();
        assertThrows(IllegalStateException.class, exam::start);
    }

    @Test
    void finishMovesFromInProgressToFinished() {
        Exam exam = newExam();
        exam.start();
        exam.finish();
        assertEquals(ExamStatus.FINISHED, exam.getStatus());
    }

    @Test
    void finishFailsWhenNotInProgress() {
        Exam exam = newExam();
        assertThrows(IllegalStateException.class, exam::finish);
    }

    @Test
    void startSessionForAddsSessionForStudent() {
        Exam exam = newExam();
        UserId studentId = new UserId();

        ExamSession session = exam.startSessionFor(studentId);

        assertEquals(1, exam.getSessions().size());
        assertEquals(studentId, session.getStudentId());
        assertEquals(exam.getId(), session.getExamId());
        assertEquals(ExamSessionStatus.IN_PROGRESS, session.getStatus());
    }

    @Test
    void finishSessionMarksMatchingSessionAsFinished() {
        Exam exam = newExam();
        ExamSession session = exam.startSessionFor(new UserId());

        exam.finishSession(session.getId());

        assertEquals(ExamSessionStatus.FINISHED, session.getStatus());
        assertNotNull(session.getEndTime());
    }

    @Test
    void finishSessionThrowsWhenSessionIdUnknown() {
        Exam exam = newExam();
        assertThrows(IllegalArgumentException.class, () -> exam.finishSession(new ExamSessionId()));
    }
}
