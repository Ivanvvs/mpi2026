package com.exam.dto;

import com.exam.model.Vote;

public class VoteDTO {

    private Long id;
    private Long votingId;

    public static VoteDTO from(Vote vote) {
        VoteDTO response = new VoteDTO();
        response.id = vote.getId();
        response.votingId = vote.getVotingId();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getVotingId() {
        return votingId;
    }
}
