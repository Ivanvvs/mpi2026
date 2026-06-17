package com.exam.dto;

import jakarta.validation.constraints.NotBlank;

public class VotingOptionRequest {

    @NotBlank
    private String label;

    private Long candidateUserId;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getCandidateUserId() {
        return candidateUserId;
    }

    public void setCandidateUserId(Long candidateUserId) {
        this.candidateUserId = candidateUserId;
    }
}
