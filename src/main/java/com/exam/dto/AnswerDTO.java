package com.exam.dto;

import com.exam.model.Answer;

import java.time.LocalDateTime;

public class AnswerDTO {

    private Long id;
    private Long sessionId;
    private Long userId;
    private Long questionId;
    private String text;
    private LocalDateTime savedAt;
    private Integer score;
    private boolean finalSubmitted;

    public static AnswerDTO from(Answer answer) {
        AnswerDTO response = new AnswerDTO();
        response.id = answer.getId();
        response.sessionId = answer.getSessionId();
        response.userId = answer.getUserId();
        response.questionId = answer.getQuestionId();
        response.text = answer.getText();
        response.savedAt = answer.getSavedAt();
        response.score = answer.getScore();
        response.finalSubmitted = answer.isFinalSubmitted();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public Integer getScore() {
        return score;
    }

    public boolean isFinalSubmitted() {
        return finalSubmitted;
    }
}
