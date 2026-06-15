package com.exam.dto;

import jakarta.validation.constraints.NotNull;

public class SubmitVoteRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long optionId;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
}
