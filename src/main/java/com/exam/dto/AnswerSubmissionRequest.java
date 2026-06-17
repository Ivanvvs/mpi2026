package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public abstract class AnswerSubmissionRequest {

    @NotNull
    private Long questionId;

    @NotBlank
    private String text;

    private boolean finalSubmitted;

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFinalSubmitted() {
        return finalSubmitted;
    }

    public void setFinalSubmitted(boolean finalSubmitted) {
        this.finalSubmitted = finalSubmitted;
    }
}
