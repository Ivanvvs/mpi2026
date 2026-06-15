package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "exam_attempts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"})
)
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ExamSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime submittedAt;

    public Long getId() {
        return id;
    }

    public ExamSession getSession() {
        return session;
    }

    public void setSession(ExamSession session) {
        this.session = session;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }
}
