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
import com.exam.repository.VoteRepository;
import com.exam.repository.VotingOptionRepository;
import com.exam.repository.VotingReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static com.exam.util.DateTimeUtils.nowUtc;

@Service
public class VotingParticipationService {

    private final VotingLifecycleService lifecycleService;
    private final VoteRepository voteRepository;
    private final VotingOptionRepository optionRepository;
    private final VotingReceiptRepository receiptRepository;
    private final CurrentUserService currentUserService;

    public VotingParticipationService(
            VotingLifecycleService lifecycleService,
            VoteRepository voteRepository,
            VotingOptionRepository optionRepository,
            VotingReceiptRepository receiptRepository,
            CurrentUserService currentUserService
    ) {
        this.lifecycleService = lifecycleService;
        this.voteRepository = voteRepository;
        this.optionRepository = optionRepository;
        this.receiptRepository = receiptRepository;
        this.currentUserService = currentUserService;
    }

    public List<VotingOption> getOptions(Long votingId) {
        lifecycleService.getVoting(votingId);
        return optionRepository.findByVoting_Id(votingId);
    }

    @Transactional
    public Vote submitVote(Long votingId, SubmitVoteRequest request) {
        User currentUser = currentUserService.getProfile();
        if (!currentUser.getId().equals(request.getStudentId())) {
            throw new BadRequestException("A student can only submit their own vote");
        }
        return submitVoteForStudent(votingId, request.getOptionId(), currentUser);
    }

    @Transactional
    public Vote submitCurrentUserVote(Long votingId, Long optionId) {
        return submitVoteForStudent(votingId, optionId, currentUserService.getProfile());
    }

    private Vote submitVoteForStudent(Long votingId, Long optionId, User student) {
        SecretVoting voting = lifecycleService.getVotingForUpdate(votingId);

        if (voting.getStatus() != VotingStatus.ACTIVE) {
            throw new BadRequestException("Voting is already finished");
        }

        if (voting.getEndsAt() != null && voting.getEndsAt().isBefore(nowUtc())) {
            lifecycleService.finishIfExpired(voting);
            throw new BadRequestException("Voting time is over");
        }

        validateStudentCanVote(voting, student);

        VotingOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting option was not found"));
        if (!option.getVoting().getId().equals(votingId)) {
            throw new BadRequestException("Voting option does not belong to this voting");
        }

        if (receiptRepository.existsByVotingIdAndStudentId(votingId, student.getId())) {
            throw new BadRequestException("Student has already voted");
        }

        Vote vote = new Vote();
        vote.setVoting(voting);
        vote.setOption(option);
        VotingReceipt receipt = new VotingReceipt();
        receipt.setVoting(voting);
        receipt.setStudent(student);
        try {
            voteRepository.save(vote);
            // Flush both records inside this transaction so a duplicate request is translated,
            // while Vote and VotingReceipt still commit or roll back together.
            receiptRepository.saveAndFlush(receipt);
        } catch (DataIntegrityViolationException exception) {
            throw new BadRequestException("Student has already voted");
        }

        return vote;
    }

    public boolean hasCurrentUserVoted(Long votingId) {
        User user = currentUserService.getProfile();
        return receiptRepository.existsByVotingIdAndStudentId(votingId, user.getId());
    }

    public List<Vote> getVotes(Long votingId) {
        lifecycleService.getVoting(votingId);
        return voteRepository.findByVoting_Id(votingId);
    }

    public void validateStudentCanViewVoting(SecretVoting voting, User student) {
        validateStudentAccess(
                voting,
                student,
                "Inactive student cannot access voting",
                "Only students can access student voting view"
        );
    }

    private void validateStudentCanVote(SecretVoting voting, User student) {
        validateStudentAccess(
                voting,
                student,
                "Inactive student cannot vote",
                "Only students can vote"
        );
    }

    private void validateStudentAccess(
            SecretVoting voting,
            User student,
            String inactiveMessage,
            String roleMessage
    ) {
        if (!student.isActive()) {
            throw new BadRequestException(inactiveMessage);
        }

        if (student.getAccount() == null || student.getAccount().getRole() != Role.STUDENT) {
            throw new BadRequestException(roleMessage);
        }

        if (student.getSchoolClass() == null || !voting.getSchoolClass().getId().equals(student.getSchoolClass().getId())) {
            throw new BadRequestException("Student does not belong to voting class");
        }
    }

}
