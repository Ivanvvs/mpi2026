package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.CreateVotingRequest;
import com.exam.dto.VotingOptionRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.SchoolClass;
import com.exam.model.SecretVoting;
import com.exam.model.User;
import com.exam.model.VotingOption;
import com.exam.model.VotingStatus;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.SecretVotingRepository;
import com.exam.repository.VotingOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VotingLifecycleService {

    private final SecretVotingRepository votingRepository;
    private final VotingOptionRepository optionRepository;
    private final SchoolClassRepository classRepository;
    private final CurrentUserService currentUserService;

    public VotingLifecycleService(
            SecretVotingRepository votingRepository,
            VotingOptionRepository optionRepository,
            SchoolClassRepository classRepository,
            CurrentUserService currentUserService
    ) {
        this.votingRepository = votingRepository;
        this.optionRepository = optionRepository;
        this.classRepository = classRepository;
        this.currentUserService = currentUserService;
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
        voting.setCreatedBy(currentUserService.getAccount());
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
        User user = currentUserService.getProfile();
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

    @Transactional
    public void finishExpiredVotings() {
        votingRepository.findByStatus(VotingStatus.ACTIVE).forEach(this::finishIfExpired);
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
}
