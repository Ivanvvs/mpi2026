package com.exam.controller;

import com.exam.dto.CreateVotingRequest;
import com.exam.dto.SubmitOwnVoteRequest;
import com.exam.dto.SubmitVoteRequest;
import com.exam.dto.VotingDetailsResponse;
import com.exam.model.Vote;
import com.exam.model.SecretVoting;
import com.exam.model.VotingOption;
import com.exam.service.VotingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vote")
@CrossOrigin(origins = "*")
public class VotingController {

    @Autowired
    private VotingService votingService;

    @PostMapping("/{sessionId}")
    public Vote vote(
            @PathVariable Long sessionId,
            @RequestBody Vote vote
    ) {
        return votingService.submitVote(sessionId, vote);
    }

    @GetMapping("/{sessionId}/results")
    public Object results(@PathVariable Long sessionId) {
        return votingService.getLegacyResults(sessionId);
    }

    @PostMapping("/secret")
    public SecretVoting createVoting(@Valid @RequestBody CreateVotingRequest request) {
        return votingService.createVoting(request);
    }

    @GetMapping("/secret")
    public List<SecretVoting> listVotings(@RequestParam(required = false) Long classId) {
        if (classId != null) {
            return votingService.getVotingsForClass(classId);
        }
        return votingService.getVotings();
    }

    @GetMapping("/secret/my")
    public List<SecretVoting> myVotings() {
        return votingService.getVotingsForCurrentUser();
    }

    @GetMapping("/secret/{votingId}")
    public VotingDetailsResponse votingDetails(@PathVariable Long votingId) {
        return votingService.getDetails(votingId);
    }

    @GetMapping("/secret/{votingId}/options")
    public List<VotingOption> votingOptions(@PathVariable Long votingId) {
        return votingService.getOptions(votingId);
    }

    @PostMapping("/secret/{votingId}/votes")
    public Vote submitSecretVote(
            @PathVariable Long votingId,
            @Valid @RequestBody SubmitVoteRequest request
    ) {
        return votingService.submitVote(votingId, request);
    }

    @PostMapping("/secret/{votingId}/votes/me")
    public Vote submitMySecretVote(
            @PathVariable Long votingId,
            @Valid @RequestBody SubmitOwnVoteRequest request
    ) {
        return votingService.submitCurrentUserVote(votingId, request.getOptionId());
    }

    @PostMapping("/secret/{votingId}/finish")
    public SecretVoting finishVoting(@PathVariable Long votingId) {
        return votingService.finishVoting(votingId);
    }

    @GetMapping("/secret/{votingId}/results")
    public Map<String, Long> secretResults(@PathVariable Long votingId) {
        return votingService.getResults(votingId);
    }
}
