package com.eduverify.platform.academicmanagement.infrastructure.persistence;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryExamRepositoryTest {
    private final InMemoryExamRepository repository = new InMemoryExamRepository();

    private Exam newExam() {
        return new Exam(new UserId(), "Quiz", LocalDateTime.now().plusDays(1), 45);
    }

    @Test
    void savesAndFindsExamById() {
        Exam exam = newExam();
        repository.save(exam);

        assertTrue(repository.findById(exam.getId()).isPresent());
        assertEquals(exam.getTitle(), repository.findById(exam.getId()).get().getTitle());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(new ExamId()).isEmpty());
    }

    @Test
    void findAllReturnsAllSavedExams() {
        repository.save(newExam());
        repository.save(newExam());

        assertEquals(2, repository.findAll().size());
    }
}
