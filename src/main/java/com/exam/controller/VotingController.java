package com.exam.controller;

import com.exam.dto.CreateVotingRequest;
import com.exam.dto.SubmitOwnVoteRequest;
import com.exam.dto.SubmitVoteRequest;
import com.exam.dto.SecretVotingResponse;
import com.exam.dto.VoteDTO;
import com.exam.dto.VotingDetailsResponse;
import com.exam.dto.VotingOptionResponse;
import com.exam.service.VotingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vote")
public class VotingController {

    private final VotingService votingService;

    public VotingController(VotingService votingService) {
        this.votingService = votingService;
    }

    @PostMapping("/secret")
    public SecretVotingResponse createVoting(@Valid @RequestBody CreateVotingRequest request) {
        return SecretVotingResponse.from(votingService.createVoting(request));
    }

    @GetMapping("/secret")
    public List<SecretVotingResponse> listVotings(@RequestParam(required = false) Long classId) {
        if (classId != null) {
            return votingService.getVotingsForClass(classId).stream().map(SecretVotingResponse::from).toList();
        }
        return votingService.getVotingsForCurrentUser().stream().map(SecretVotingResponse::from).toList();
    }

    @GetMapping("/secret/my")
    public List<SecretVotingResponse> myVotings() {
        return votingService.getVotingsForCurrentUser().stream().map(SecretVotingResponse::from).toList();
    }

    @GetMapping("/secret/{votingId}")
    public VotingDetailsResponse votingDetails(@PathVariable Long votingId) {
        return votingService.getDetails(votingId);
    }

    @GetMapping("/secret/{votingId}/options")
    public List<VotingOptionResponse> votingOptions(@PathVariable Long votingId) {
        return votingService.getOptions(votingId).stream().map(VotingOptionResponse::from).toList();
    }

    @PostMapping("/secret/{votingId}/votes")
    public VoteDTO submitSecretVote(
            @PathVariable Long votingId,
            @Valid @RequestBody SubmitVoteRequest request
    ) {
        return VoteDTO.from(votingService.submitVote(votingId, request));
    }

    @PostMapping("/secret/{votingId}/votes/me")
    public VoteDTO submitMySecretVote(
            @PathVariable Long votingId,
            @Valid @RequestBody SubmitOwnVoteRequest request
    ) {
        return VoteDTO.from(votingService.submitCurrentUserVote(votingId, request.getOptionId()));
    }

    @PostMapping("/secret/{votingId}/finish")
    public SecretVotingResponse finishVoting(@PathVariable Long votingId) {
        return SecretVotingResponse.from(votingService.finishVoting(votingId));
    }

    @GetMapping("/secret/{votingId}/results")
    public Map<String, Long> secretResults(@PathVariable Long votingId) {
        return votingService.getResults(votingId);
    }
}
