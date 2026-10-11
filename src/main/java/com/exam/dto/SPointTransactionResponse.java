package com.exam.dto;

import com.exam.model.SPointTransaction;

import java.time.LocalDateTime;

public record SPointTransactionResponse(
        String description,
        String examTitle,
        String examSubject,
        int pointsDelta,
        int resultingBalance,
        LocalDateTime operationAt
) {
    public static SPointTransactionResponse from(SPointTransaction transaction) {
        boolean examResult = "EXAM_RESULT".equals(transaction.getReason());
        return new SPointTransactionResponse(
                examResult ? "Начисление по результату экзамена" : "Корректировка S-очков",
                transaction.getSession() == null ? null : transaction.getSession().getTitle(),
                transaction.getSession() == null ? null : transaction.getSession().getSubject(),
                transaction.getPointsDelta(),
                transaction.getResultingBalance(),
                transaction.getCreatedAt()
        );
    }
}
