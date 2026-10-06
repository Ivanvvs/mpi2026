package com.exam.dto;

import com.exam.model.ClassRank;

import java.util.List;

public record RankDetailsResponse(
        Long classId,
        String className,
        ClassRank currentRank,
        ClassRank proposedRank,
        int totalSPoints,
        boolean rankChangeRequired,
        int proposedRankMinimumSPoints,
        ClassRank nextHigherRank,
        Integer pointsToNextHigherRank,
        int calculatedStudentsSPoints,
        List<RankDetailsStudentResponse> students
) {
}
