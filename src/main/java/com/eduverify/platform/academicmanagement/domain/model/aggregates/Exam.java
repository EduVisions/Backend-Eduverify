package com.eduverify.platform.academicmanagement.domain.model.aggregates;

import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamSessionId;
import com.eduverify.platform.academicmanagement.domain.model.valueobjects.ExamStatus;
import com.eduverify.platform.shared.domain.model.valueobjects.UserId;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class Exam {
    private final ExamId id;
    private final UserId teacherId;
    @Setter @NonNull private String title;
    private final LocalDateTime scheduledDate;
    private final int durationMinutes;
    private ExamStatus status;
    private final List<ExamSession> sessions;

    public Exam(@NonNull UserId teacherId, String title, @NonNull LocalDateTime scheduledDate, int durationMinutes) {
        if (Objects.isNull(title) || title.isBlank())
            throw new IllegalArgumentException("Exam title cannot be null or blank");
        if (durationMinutes <= 0)
            throw new IllegalArgumentException("Exam duration must be greater than zero");

        this.id = new ExamId();
        this.teacherId = teacherId;
        this.title = title;
        this.scheduledDate = scheduledDate;
        this.durationMinutes = durationMinutes;
        this.status = ExamStatus.SCHEDULED;
        this.sessions = new ArrayList<>();
    }

    public void start() {
        if (status != ExamStatus.SCHEDULED)
            throw new IllegalStateException("Only a scheduled exam can be started");
        this.status = ExamStatus.IN_PROGRESS;
    }

    public void finish() {
        if (status != ExamStatus.IN_PROGRESS)
            throw new IllegalStateException("Only an in-progress exam can be finished");
        this.status = ExamStatus.FINISHED;
    }

    public ExamSession startSessionFor(@NonNull UserId studentId) {
        ExamSession session = new ExamSession(this.id, studentId);
        this.sessions.add(session);
        return session;
    }

    public void finishSession(@NonNull ExamSessionId sessionId) {
        ExamSession session = sessions.stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No session found with id: " + sessionId));
        session.finish();
    }
}
