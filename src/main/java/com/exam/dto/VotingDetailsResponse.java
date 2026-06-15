package com.exam.dto;

import com.exam.model.SecretVoting;
import com.exam.model.VotingOption;

import java.util.List;
import java.util.Map;

public class VotingDetailsResponse {

    private SecretVoting voting;
    private List<VotingOption> options;
    private Map<String, Long> results;
    private boolean hasVoted;
    private boolean resultsVisible;

    public VotingDetailsResponse(
            SecretVoting voting,
            List<VotingOption> options,
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

    public SecretVoting getVoting() {
        return voting;
    }

    public List<VotingOption> getOptions() {
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
