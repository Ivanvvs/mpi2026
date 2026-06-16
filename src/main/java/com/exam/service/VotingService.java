package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.auth.Role;
import com.exam.dto.CreateVotingRequest;
import com.exam.dto.SubmitVoteRequest;
import com.exam.dto.VotingDetailsResponse;
import com.exam.dto.VotingOptionRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.SchoolClass;
import com.exam.model.SecretVoting;
import com.exam.model.User;
import com.exam.model.Vote;
import com.exam.model.VotingOption;
import com.exam.model.VotingReceipt;
import com.exam.model.VotingStatus;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.SecretVotingRepository;
import com.exam.repository.UserRepository;
import com.exam.repository.VoteRepository;
import com.exam.repository.VotingOptionRepository;
import com.exam.repository.VotingReceiptRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final SecretVotingRepository votingRepository;
    private final VotingOptionRepository optionRepository;
    private final VotingReceiptRepository receiptRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;

    public VotingService(
            VoteRepository voteRepository,
            SecretVotingRepository votingRepository,
            VotingOptionRepository optionRepository,
            VotingReceiptRepository receiptRepository,
            SchoolClassRepository classRepository,
            UserRepository userRepository,
            AppUserRepository appUserRepository
    ) {
        this.voteRepository = voteRepository;
        this.votingRepository = votingRepository;
        this.optionRepository = optionRepository;
        this.receiptRepository = receiptRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public SecretVoting createVoting(CreateVotingRequest request) {
        if (request.getOptions().isEmpty()) {
            throw new BadRequestException("Voting must contain at least one option");
        }

        SchoolClass schoolClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class was not found"));

        SecretVoting voting = new SecretVoting();
        voting.setTitle(request.getTitle());
        voting.setDescription(request.getDescription());
        voting.setSchoolClass(schoolClass);
        voting.setEndsAt(request.getEndsAt());
        voting.setStatus(VotingStatus.ACTIVE);
        voting.setCreatedBy(currentUser());
        voting = votingRepository.save(voting);

        for (VotingOptionRequest optionRequest : request.getOptions()) {
            VotingOption option = new VotingOption();
            option.setVoting(voting);
            option.setLabel(optionRequest.getLabel());
            option.setCandidateUserId(optionRequest.getCandidateUserId());
            optionRepository.save(option);
        }

        return voting;
    }

    @Transactional
    public Vote submitVote(Long votingId, SubmitVoteRequest request) {
        SecretVoting voting = getVoting(votingId);

        if (voting.getStatus() != VotingStatus.ACTIVE) {
            throw new BadRequestException("Voting is already finished");
        }

        if (voting.getEndsAt() != null && voting.getEndsAt().isBefore(LocalDateTime.now())) {
            finishVoting(votingId);
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
    public SecretVoting finishVoting(Long votingId) {
        SecretVoting voting = getVoting(votingId);
        voting.setStatus(VotingStatus.FINISHED);
        voting.setFinishedAt(LocalDateTime.now());
        return votingRepository.save(voting);
    }

    public SecretVoting getVoting(Long votingId) {
        SecretVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting was not found"));
        return finishIfExpired(voting);
    }

    public List<SecretVoting> getVotings() {
        return votingRepository.findAll().stream()
                .map(this::finishIfExpired)
                .toList();
    }

    public List<SecretVoting> getVotingsForCurrentUser() {
        User user = currentDomainUser();
        if (user.getAccount() != null && user.getAccount().getRole() == Role.STUDENT) {
            if (user.getSchoolClass() == null) {
                return List.of();
            }
            return votingRepository.findBySchoolClassId(user.getSchoolClass().getId()).stream()
                    .map(this::finishIfExpired)
                    .toList();
        }
        return getVotings();
    }

    public List<SecretVoting> getVotingsForClass(Long classId) {
        return votingRepository.findBySchoolClassId(classId).stream()
                .map(this::finishIfExpired)
                .toList();
    }

    public List<VotingOption> getOptions(Long votingId) {
        getVoting(votingId);
        return optionRepository.findByVotingId(votingId);
    }

    public VotingDetailsResponse getDetails(Long votingId) {
        SecretVoting voting = getVoting(votingId);
        AppUser account = currentUser();
        boolean student = account.getRole() == Role.STUDENT;
        boolean hasVoted = false;
        boolean resultsVisible = !student || voting.getStatus() == VotingStatus.FINISHED;

        if (student) {
            User user = currentDomainUser();
            validateStudentCanVoteForDetails(voting, user);
            hasVoted = receiptRepository.existsByVotingIdAndStudentId(votingId, user.getId());
        }

        return new VotingDetailsResponse(
                voting,
                optionRepository.findByVotingId(votingId),
                resultsVisible ? getResults(votingId) : Map.of(),
                hasVoted,
                resultsVisible
        );
    }

    public Map<String, Long> getResults(Long votingId) {
        List<VotingOption> options = optionRepository.findByVotingId(votingId);
        Map<Long, String> optionLabels = new LinkedHashMap<>();
        Map<String, Long> results = new LinkedHashMap<>();

        for (VotingOption option : options) {
            optionLabels.put(option.getId(), option.getLabel());
            results.put(option.getLabel(), 0L);
        }

        for (Vote vote : voteRepository.findByVotingId(votingId)) {
            Long optionId = decodeVote(vote.getEncryptedValue());
            String label = optionLabels.get(optionId);
            if (label != null) {
                results.put(label, results.get(label) + 1);
            }
        }

        return results;
    }

    @Transactional
    public Vote submitCurrentUserVote(Long votingId, Long optionId) {
        User user = currentDomainUser();
        SubmitVoteRequest request = new SubmitVoteRequest();
        request.setStudentId(user.getId());
        request.setOptionId(optionId);
        return submitVote(votingId, request);
    }

    private String encodeVote(Long optionId) {
        return Base64.getEncoder().encodeToString(String.valueOf(optionId).getBytes(StandardCharsets.UTF_8));
    }

    private Long decodeVote(String encryptedValue) {
        String decoded = new String(Base64.getDecoder().decode(encryptedValue), StandardCharsets.UTF_8);
        return Long.valueOf(decoded);
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

    private SecretVoting finishIfExpired(SecretVoting voting) {
        if (voting.getStatus() == VotingStatus.ACTIVE
                && voting.getEndsAt() != null
                && voting.getEndsAt().isBefore(LocalDateTime.now())) {
            voting.setStatus(VotingStatus.FINISHED);
            voting.setFinishedAt(voting.getEndsAt());
            return votingRepository.save(voting);
        }
        return voting;
    }

    @Transactional
    public void finishExpiredVotings() {
        votingRepository.findByStatus(VotingStatus.ACTIVE).forEach(this::finishIfExpired);
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private User currentDomainUser() {
        AppUser account = currentUser();
        return userRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile was not found"));
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

    private void validateStudentCanVoteForDetails(SecretVoting voting, User student) {
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
}
