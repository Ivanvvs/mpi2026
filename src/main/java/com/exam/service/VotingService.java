package com.exam.service;

import com.exam.dto.CreateVotingRequest;
import com.exam.dto.SubmitVoteRequest;
import com.exam.dto.VotingDetailsResponse;
import com.exam.model.SecretVoting;
import com.exam.model.Vote;
import com.exam.model.VotingOption;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VotingService {

    private final VotingLifecycleService lifecycleService;
    private final VotingParticipationService participationService;
    private final VotingResultService resultService;

    public VotingService(
            VotingLifecycleService lifecycleService,
            VotingParticipationService participationService,
            VotingResultService resultService
    ) {
        this.lifecycleService = lifecycleService;
        this.participationService = participationService;
        this.resultService = resultService;
    }

    public SecretVoting createVoting(CreateVotingRequest request) {
        return lifecycleService.createVoting(request);
    }

    public List<SecretVoting> getVotings() {
        return lifecycleService.getVotings();
    }

    public List<SecretVoting> getVotingsForCurrentUser() {
        return lifecycleService.getVotingsForCurrentUser();
    }

    public List<SecretVoting> getVotingsForClass(Long classId) {
        return lifecycleService.getVotingsForClass(classId);
    }

    public VotingDetailsResponse getDetails(Long votingId) {
        return resultService.getDetails(votingId);
    }

    public List<VotingOption> getOptions(Long votingId) {
        return participationService.getOptions(votingId);
    }

    public Vote submitVote(Long votingId, SubmitVoteRequest request) {
        return participationService.submitVote(votingId, request);
    }

    public Vote submitCurrentUserVote(Long votingId, Long optionId) {
        return participationService.submitCurrentUserVote(votingId, optionId);
    }

    public SecretVoting finishVoting(Long votingId) {
        return lifecycleService.finishVoting(votingId);
    }

    public Map<String, Long> getResults(Long votingId) {
        return resultService.getResults(votingId);
    }

    public void finishExpiredVotings() {
        lifecycleService.finishExpiredVotings();
    }
}
