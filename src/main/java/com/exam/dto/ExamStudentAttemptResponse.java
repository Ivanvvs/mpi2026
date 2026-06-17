package com.exam.dto;

import com.exam.model.ExamAttempt;

import java.time.LocalDateTime;

public class ExamStudentAttemptResponse {

    private Long studentId;
    private boolean started;
    private boolean submitted;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    public static ExamStudentAttemptResponse from(ExamAttempt attempt) {
        ExamStudentAttemptResponse response = new ExamStudentAttemptResponse();
        response.studentId = attempt.getStudent().getId();
        response.started = true;
        response.submitted = attempt.isSubmitted();
        response.startedAt = attempt.getStartedAt();
        response.submittedAt = attempt.getSubmittedAt();
        return response;
    }

    public Long getStudentId() {
        return studentId;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
