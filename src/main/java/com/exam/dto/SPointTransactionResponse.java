package com.exam.dto;

import com.exam.model.SPointTransaction;

import java.time.LocalDateTime;

public record SPointTransactionResponse(
        String source,
        int pointsDelta,
        int resultingBalance,
        LocalDateTime operationAt
) {
    public static SPointTransactionResponse from(SPointTransaction transaction) {
        return new SPointTransactionResponse(
                transaction.getReason(),
                transaction.getPointsDelta(),
                transaction.getResultingBalance(),
                transaction.getCreatedAt()
        );
    }
}
