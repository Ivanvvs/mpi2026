package com.exam.dto;

import java.util.List;

public record StudentSPointHistoryResponse(
        Long studentId,
        int totalSPoints,
        int examResultsTotal,
        List<StudentExamResultResponse> examResults,
        List<SPointTransactionResponse> transactions
) {
}
