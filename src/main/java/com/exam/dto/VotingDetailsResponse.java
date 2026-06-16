package com.exam.dto;

import java.util.List;
import java.util.Map;

public class VotingDetailsResponse {

    private SecretVotingResponse voting;
    private List<VotingOptionResponse> options;
    private Map<String, Long> results;
    private boolean hasVoted;
    private boolean resultsVisible;

    public VotingDetailsResponse(
            SecretVotingResponse voting,
            List<VotingOptionResponse> options,
            Map<String, Long> results,
            boolean hasVoted,
            boolean resultsVisible
    ) {
        this.voting = voting;
        this.options = options;
        this.results = results;
        this.hasVoted = hasVoted;
        this.resultsVisible = resultsVisible;
    }

    public SecretVotingResponse getVoting() {
        return voting;
    }

    public List<VotingOptionResponse> getOptions() {
        return options;
    }

    public Map<String, Long> getResults() {
        return results;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public boolean isResultsVisible() {
        return resultsVisible;
    }
}
