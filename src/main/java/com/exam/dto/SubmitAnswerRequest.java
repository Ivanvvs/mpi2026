package com.exam.dto;

import jakarta.validation.constraints.NotNull;

public class SubmitAnswerRequest extends AnswerSubmissionRequest {

    @NotNull
    private Long studentId;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
