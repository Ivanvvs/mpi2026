package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubmitAnswerRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long questionId;

    @NotBlank
    private String text;

    private boolean finalSubmitted;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

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
