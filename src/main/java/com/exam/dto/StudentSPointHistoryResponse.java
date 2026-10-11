package com.exam.dto;

import java.util.List;

public record StudentSPointHistoryResponse(
        Long studentId,
        int totalSPoints,
        List<SPointTransactionResponse> transactions
) {
}
