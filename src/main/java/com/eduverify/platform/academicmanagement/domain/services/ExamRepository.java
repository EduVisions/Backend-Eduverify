package com.eduverify.platform.academicmanagement.domain.services;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;

import java.util.List;
import java.util.Optional;

public interface ExamRepository {
    Exam save(Exam exam);
    Optional<Exam> findById(ExamId id);
    List<Exam> findAll();
}
