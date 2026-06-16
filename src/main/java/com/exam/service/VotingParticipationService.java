package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.SubmitVoteRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.SecretVoting;
import com.exam.model.User;
import com.exam.model.Vote;
import com.exam.model.VotingOption;
import com.exam.model.VotingReceipt;
import com.exam.model.VotingStatus;
import com.exam.repository.UserRepository;
import com.exam.repository.VoteRepository;
import com.exam.repository.VotingOptionRepository;
import com.exam.repository.VotingReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class VotingParticipationService {

    private final VotingLifecycleService lifecycleService;
    private final VoteRepository voteRepository;
    private final VotingOptionRepository optionRepository;
    private final VotingReceiptRepository receiptRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public VotingParticipationService(
            VotingLifecycleService lifecycleService,
            VoteRepository voteRepository,
            VotingOptionRepository optionRepository,
            VotingReceiptRepository receiptRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.lifecycleService = lifecycleService;
        this.voteRepository = voteRepository;
        this.optionRepository = optionRepository;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<VotingOption> getOptions(Long votingId) {
        lifecycleService.getVoting(votingId);
        return optionRepository.findByVotingId(votingId);
    }

    @Transactional
    public Vote submitVote(Long votingId, SubmitVoteRequest request) {
        SecretVoting voting = lifecycleService.getVoting(votingId);

        if (voting.getStatus() != VotingStatus.ACTIVE) {
            throw new BadRequestException("Voting is already finished");
        }

        if (voting.getEndsAt() != null && voting.getEndsAt().isBefore(LocalDateTime.now())) {
            lifecycleService.finishVoting(votingId);
            throw new BadRequestException("Voting time is over");
        }

        User student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student was not found"));
        validateStudentCanVote(voting, student);

        VotingOption option = optionRepository.findById(request.getOptionId())
                .orElseThrow(() -> new ResourceNotFoundException("Voting option was not found"));
        if (!option.getVoting().getId().equals(votingId)) {
            throw new BadRequestException("Voting option does not belong to this voting");
        }

        if (receiptRepository.existsByVotingIdAndStudentId(votingId, student.getId())) {
            throw new BadRequestException("Student has already voted");
        }

        Vote vote = new Vote();
        vote.setVotingId(votingId);
        vote.setEncryptedValue(encodeVote(option.getId()));
        vote.setAnonymousVoterHash(hash(votingId + ":" + student.getId()));
        voteRepository.save(vote);

        VotingReceipt receipt = new VotingReceipt();
        receipt.setVoting(voting);
        receipt.setStudent(student);
        receiptRepository.save(receipt);

        return vote;
    }

    @Transactional
    public Vote submitCurrentUserVote(Long votingId, Long optionId) {
        User user = currentUserService.getProfile();
        SubmitVoteRequest request = new SubmitVoteRequest();
        request.setStudentId(user.getId());
        request.setOptionId(optionId);
        return submitVote(votingId, request);
    }

    public boolean hasCurrentUserVoted(Long votingId) {
        User user = currentUserService.getProfile();
        return receiptRepository.existsByVotingIdAndStudentId(votingId, user.getId());
    }

    public List<Vote> getVotes(Long votingId) {
        lifecycleService.getVoting(votingId);
        return voteRepository.findByVotingId(votingId);
    }

    public void validateStudentCanViewVoting(SecretVoting voting, User student) {
        if (!student.isActive()) {
            throw new BadRequestException("Inactive student cannot access voting");
        }

        if (student.getAccount() == null || student.getAccount().getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can access student voting view");
        }

        if (student.getSchoolClass() == null || !voting.getSchoolClass().getId().equals(student.getSchoolClass().getId())) {
            throw new BadRequestException("Student does not belong to voting class");
        }
    }

    private void validateStudentCanVote(SecretVoting voting, User student) {
        if (!student.isActive()) {
            throw new BadRequestException("Inactive student cannot vote");
        }

        if (student.getAccount() == null || student.getAccount().getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can vote");
        }

        if (student.getSchoolClass() == null || !voting.getSchoolClass().getId().equals(student.getSchoolClass().getId())) {
            throw new BadRequestException("Student does not belong to voting class");
        }
    }

    private String encodeVote(Long optionId) {
        return Base64.getEncoder().encodeToString(String.valueOf(optionId).getBytes(StandardCharsets.UTF_8));
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
