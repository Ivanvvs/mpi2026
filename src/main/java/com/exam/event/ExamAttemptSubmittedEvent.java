package com.exam.event;

public record ExamAttemptSubmittedEvent(Long examId, Long userId) {
}
