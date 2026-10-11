package com.exam.event;

public record ExamAnswerSavedEvent(Long examId, Long userId) {
}
