package com.exam.dto;

import com.exam.model.Question;
import com.exam.model.QuestionType;

public class QuestionResponse {

    private Long id;
    private Long sessionId;
    private Integer orderIndex;
    private String text;
    private QuestionType type;
    private String optionsJson;
    private String correctAnswer;
    private Integer maxScore;
    private String metadataJson;

    public static QuestionResponse from(Question question, boolean includeCorrectAnswer) {
        QuestionResponse response = new QuestionResponse();
        response.id = question.getId();
        response.sessionId = question.getSessionId();
        response.orderIndex = question.getOrderIndex();
        response.text = question.getText();
        response.type = question.getType();
        response.optionsJson = question.getOptionsJson();
        response.correctAnswer = includeCorrectAnswer ? question.getCorrectAnswer() : null;
        response.maxScore = question.getMaxScore();
        response.metadataJson = question.getMetadataJson();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public String getText() {
        return text;
    }

    public QuestionType getType() {
        return type;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public String getMetadataJson() {
        return metadataJson;
    }
}
