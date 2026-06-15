package com.exam.dto;

import com.exam.model.SecretVoting;
import com.exam.model.VotingOption;

import java.util.List;
import java.util.Map;

public class VotingDetailsResponse {

    private SecretVoting voting;
    private List<VotingOption> options;
    private Map<String, Long> results;

    public VotingDetailsResponse(SecretVoting voting, List<VotingOption> options, Map<String, Long> results) {
        this.voting = voting;
        this.options = options;
        this.results = results;
    }

    public SecretVoting getVoting() {
        return voting;
    }

    public List<VotingOption> getOptions() {
        return options;
    }

    public Map<String, Long> getResults() {
        return results;
    }
}
