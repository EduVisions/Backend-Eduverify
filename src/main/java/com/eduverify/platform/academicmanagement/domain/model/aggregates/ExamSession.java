package com.eduverify.platform.academicmanagement.domain.model.aggregates;

import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionStatus;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import lombok.Getter;
import lombok.NonNull;

import java.time.LocalDateTime;

@Getter
public class ExamSession {
    private final ExamSessionId id;
    private final ExamId examId;
    private final UserId studentId;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private ExamSessionStatus status;

    ExamSession(@NonNull ExamId examId, @NonNull UserId studentId) {
        this.id = new ExamSessionId();
        this.examId = examId;
        this.studentId = studentId;
        this.startTime = LocalDateTime.now();
        this.status = ExamSessionStatus.IN_PROGRESS;
    }

    void finish() {
        if (status == ExamSessionStatus.FINISHED)
            throw new IllegalStateException("Exam session is already finished");
        this.status = ExamSessionStatus.FINISHED;
        this.endTime = LocalDateTime.now();
    }
}
