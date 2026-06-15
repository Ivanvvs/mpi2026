package com.exam.dto;

import com.exam.model.ExamAttempt;

import java.time.LocalDateTime;

public class ExamAttemptResponse {

    private boolean started;
    private boolean submitted;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    public static ExamAttemptResponse empty() {
        return new ExamAttemptResponse();
    }

    public static ExamAttemptResponse from(ExamAttempt attempt) {
        ExamAttemptResponse response = new ExamAttemptResponse();
        response.started = true;
        response.submitted = attempt.isSubmitted();
        response.startedAt = attempt.getStartedAt();
        response.submittedAt = attempt.getSubmittedAt();
        return response;
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
