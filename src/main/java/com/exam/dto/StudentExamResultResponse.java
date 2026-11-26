package com.exam.dto;

import com.exam.model.ExamResult;

import java.time.LocalDateTime;

public record StudentExamResultResponse(
        Long examId,
        String examTitle,
        String subject,
        LocalDateTime gradedAt,
        int finalScore,
        int maxScore
) {
    public static StudentExamResultResponse from(ExamResult result, int maxScore) {
        return new StudentExamResultResponse(
                result.getSession().getId(),
                result.getSession().getTitle(),
                result.getSession().getSubject(),
                result.getGradedAt(),
                result.getFinalScore(),
                maxScore
        );
    }
}
