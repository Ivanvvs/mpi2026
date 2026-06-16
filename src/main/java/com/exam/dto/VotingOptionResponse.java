package com.exam.dto;

import com.exam.model.VotingOption;

public class VotingOptionResponse {

    private Long id;
    private String label;
    private Long candidateUserId;

    public static VotingOptionResponse from(VotingOption option) {
        VotingOptionResponse response = new VotingOptionResponse();
        response.id = option.getId();
        response.label = option.getLabel();
        response.candidateUserId = option.getCandidateUserId();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public Long getCandidateUserId() {
        return candidateUserId;
    }
}
