package com.exam.dto;

import jakarta.validation.constraints.NotNull;

public class SubmitOwnVoteRequest {

    @NotNull
    private Long optionId;

    public Long getOptionId() {
        return optionId;
    }

    public void setOptionId(Long optionId) {
        this.optionId = optionId;
    }
}
