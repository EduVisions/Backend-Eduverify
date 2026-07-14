package com.eduverify.platform.academicmanagement.infrastructure.persistence;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.services.ExamRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryExamRepository implements ExamRepository {
    private final Map<ExamId, Exam> examsById = new ConcurrentHashMap<>();

    @Override
    public Exam save(Exam exam) {
        examsById.put(exam.getId(), exam);
        return exam;
    }

    @Override
    public Optional<Exam> findById(ExamId id) {
        return Optional.ofNullable(examsById.get(id));
    }

    @Override
    public List<Exam> findAll() {
        return new ArrayList<>(examsById.values());
    }
}
