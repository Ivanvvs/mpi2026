package com.exam.event;

public record ExamFinishedEvent(Long examId, String message) {
}
