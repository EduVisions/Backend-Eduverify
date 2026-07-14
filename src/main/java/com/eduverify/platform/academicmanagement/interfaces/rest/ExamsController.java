package com.eduverify.platform.academicmanagement.interfaces.rest;

import com.eduverify.platform.academicmanagement.domain.model.aggregates.Exam;
import com.eduverify.platform.academicmanagement.domain.model.aggregates.ExamSession;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.services.ExamService;
import com.eduverify.platform.academicmanagement.interfaces.rest.resources.CreateExamResource;
import com.eduverify.platform.academicmanagement.interfaces.rest.resources.ExamResource;
import com.eduverify.platform.academicmanagement.interfaces.rest.resources.ExamSessionResource;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import com.eduverify.platform.shared.infrastructure.security.CurrentUserContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/exams")
public class ExamsController {
    private final ExamService examService;

    public ExamsController(ExamService examService) {
        this.examService = examService;
    }

    @PostMapping
    public ResponseEntity<ExamResource> createExam(@RequestBody CreateExamResource resource) {
        UserId teacherId = CurrentUserContext.get();
        Exam exam = examService.createExam(
                teacherId,
                resource.title(),
                LocalDateTime.parse(resource.scheduledDate()),
                resource.durationMinutes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ExamResource.from(exam));
    }

    @GetMapping
    public List<ExamResource> listExams() {
        return examService.listExams().stream().map(ExamResource::from).toList();
    }

    @GetMapping("/{examId}")
    public ExamResource getExam(@PathVariable String examId) {
        return ExamResource.from(examService.getExam(new ExamId(UUID.fromString(examId))));
    }

    @PostMapping("/{examId}/start")
    public ExamResource startExam(@PathVariable String examId) {
        return ExamResource.from(examService.startExam(new ExamId(UUID.fromString(examId))));
    }

    @PostMapping("/{examId}/finish")
    public ExamResource finishExam(@PathVariable String examId) {
        return ExamResource.from(examService.finishExam(new ExamId(UUID.fromString(examId))));
    }

    @PostMapping("/{examId}/sessions")
    public ResponseEntity<ExamSessionResource> accessExam(@PathVariable String examId) {
        UserId studentId = CurrentUserContext.get();
        ExamSession session = examService.accessExam(new ExamId(UUID.fromString(examId)), studentId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExamSessionResource.from(session));
    }

    @PutMapping("/{examId}/sessions/{sessionId}/finish")
    public ExamResource submitExam(@PathVariable String examId, @PathVariable String sessionId) {
        return ExamResource.from(examService.submitExam(
                new ExamId(UUID.fromString(examId)),
                new ExamSessionId(UUID.fromString(sessionId))
        ));
    }
}
