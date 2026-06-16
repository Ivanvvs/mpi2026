package com.exam.dto;

import com.exam.model.ExamResult;

import java.time.LocalDateTime;

public class ExamResultResponse {

    private Long id;
    private ExamSessionDTO session;
    private UserResponse student;
    private int rawScore;
    private int violationPenalty;
    private int finalScore;
    private Integer rankPlace;
    private LocalDateTime gradedAt;

    public static ExamResultResponse from(ExamResult result) {
        ExamResultResponse response = new ExamResultResponse();
        response.id = result.getId();
        response.session = result.getSession() == null ? null : ExamSessionDTO.from(result.getSession());
        response.student = result.getStudent() == null ? null : UserResponse.from(result.getStudent());
        response.rawScore = result.getRawScore();
        response.violationPenalty = result.getViolationPenalty();
        response.finalScore = result.getFinalScore();
        response.rankPlace = result.getRankPlace();
        response.gradedAt = result.getGradedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public ExamSessionDTO getSession() {
        return session;
    }

    public UserResponse getStudent() {
        return student;
    }

    public int getRawScore() {
        return rawScore;
    }

    public int getViolationPenalty() {
        return violationPenalty;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public Integer getRankPlace() {
        return rankPlace;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }
}
