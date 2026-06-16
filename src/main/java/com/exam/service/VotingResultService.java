package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.SecretVotingResponse;
import com.exam.dto.VotingDetailsResponse;
import com.exam.dto.VotingOptionResponse;
import com.exam.model.SecretVoting;
import com.exam.model.User;
import com.exam.model.VotingOption;
import com.exam.model.VotingStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VotingResultService {

    private final VotingLifecycleService lifecycleService;
    private final VotingParticipationService participationService;
    private final CurrentUserService currentUserService;

    public VotingResultService(
            VotingLifecycleService lifecycleService,
            VotingParticipationService participationService,
            CurrentUserService currentUserService
    ) {
        this.lifecycleService = lifecycleService;
        this.participationService = participationService;
        this.currentUserService = currentUserService;
    }

    public VotingDetailsResponse getDetails(Long votingId) {
        SecretVoting voting = lifecycleService.getVoting(votingId);
        var account = currentUserService.getAccount();
        boolean student = account.getRole() == Role.STUDENT;
        boolean hasVoted = false;
        boolean resultsVisible = !student || voting.getStatus() == VotingStatus.FINISHED;

        if (student) {
            User user = currentUserService.getProfile();
            participationService.validateStudentCanViewVoting(voting, user);
            hasVoted = participationService.hasCurrentUserVoted(votingId);
        }

        return new VotingDetailsResponse(
                SecretVotingResponse.from(voting),
                participationService.getOptions(votingId).stream().map(VotingOptionResponse::from).toList(),
                resultsVisible ? getResults(votingId) : Map.of(),
                hasVoted,
                resultsVisible
        );
    }

    public Map<String, Long> getResults(Long votingId) {
        List<VotingOption> options = participationService.getOptions(votingId);
        Map<Long, String> optionLabels = new LinkedHashMap<>();
        Map<String, Long> results = new LinkedHashMap<>();

        for (VotingOption option : options) {
            optionLabels.put(option.getId(), option.getLabel());
            results.put(option.getLabel(), 0L);
        }

        for (var vote : participationService.getVotes(votingId)) {
            Long optionId = decodeVote(vote.getEncryptedValue());
            String label = optionLabels.get(optionId);
            if (label != null) {
                results.put(label, results.get(label) + 1);
            }
        }

        return results;
    }

    private Long decodeVote(String encryptedValue) {
        String decoded = new String(Base64.getDecoder().decode(encryptedValue), StandardCharsets.UTF_8);
        return Long.valueOf(decoded);
    }
}
