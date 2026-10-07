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
import com.exam.repository.UserRepository;
import com.exam.repository.VotingOptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.exam.util.DateTimeUtils.nowUtc;

@Service
public class VotingLifecycleService {

    private final SecretVotingRepository votingRepository;
    private final VotingOptionRepository optionRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AccessControlService accessControl;
    private final VotingResultService resultService;

    public VotingLifecycleService(
            SecretVotingRepository votingRepository,
            VotingOptionRepository optionRepository,
            SchoolClassRepository classRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            AccessControlService accessControl,
            VotingResultService resultService
    ) {
        this.votingRepository = votingRepository;
        this.optionRepository = optionRepository;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.accessControl = accessControl;
        this.resultService = resultService;
    }

    @Transactional
    public SecretVoting createVoting(CreateVotingRequest request) {
        assertCanCreateVoting(request.getClassId());

        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new BadRequestException("Voting must contain at least two options");
        }

        var normalizedLabels = request.getOptions().stream().map(VotingOptionRequest::getLabel)
                .map(label -> label == null ? "" : label.trim()).toList();
        if (normalizedLabels.stream().anyMatch(String::isBlank)) {
            throw new BadRequestException("Voting option label must not be blank");
        }
        if (normalizedLabels.stream().distinct().count() != normalizedLabels.size()) {
            throw new BadRequestException("Voting option labels must be unique");
        }

        if (request.getEndsAt() == null) {
            throw new BadRequestException("Voting end time is required");
        }

        if (!request.getEndsAt().isAfter(nowUtc())) {
            throw new BadRequestException("Voting end time must be in the future");
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
            option.setLabel(optionRequest.getLabel().trim());
            if (optionRequest.getCandidateUserId() != null) {
                option.setCandidateUser(userRepository.findById(optionRequest.getCandidateUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Candidate user was not found")));
            }
            optionRepository.save(option);
        }

        return voting;
    }

    @Transactional
    public SecretVoting finishVoting(Long votingId) {
        SecretVoting voting = getVotingForUpdate(votingId);
        assertCanManageVoting(voting);
        return completeVoting(voting, nowUtc());
    }

    @Transactional
    public SecretVoting getVoting(Long votingId) {
        SecretVoting voting = votingRepository.findById(votingId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting was not found"));
        assertCanViewVoting(voting);
        return finishIfExpired(voting);
    }

    @Transactional
    public List<SecretVoting> getVotings() {
        return votingRepository.findAll().stream()
                .map(this::finishIfExpired)
                .toList();
    }

    @Transactional
    public List<SecretVoting> getVotingsForCurrentUser() {
        User user = accessControl.currentProfile();
        Role role = accessControl.currentRole();
        if (role == Role.ADMIN) {
            return getVotings();
        }
        if (role == Role.CURATOR && user.getSchoolClass() != null) {
            return votingRepository.findBySchoolClassId(user.getSchoolClass().getId()).stream()
                    .map(this::finishIfExpired)
                    .toList();
        }
        if (role == Role.CURATOR && user.getAccount() != null) {
            return votingRepository.findByCreatedById(accessControl.currentAccountId()).stream()
                    .map(this::finishIfExpired)
                    .toList();
        }
        if (role != Role.STUDENT || !user.isActive() || user.getSchoolClass() == null) {
            return List.of();
        }
        return votingRepository.findBySchoolClassId(user.getSchoolClass().getId()).stream()
                .map(this::finishIfExpired)
                .toList();
    }

    @Transactional
    public List<SecretVoting> getVotingsForClass(Long classId) {
        assertCanViewClassVotings(classId);
        return votingRepository.findBySchoolClassId(classId).stream()
                .map(this::finishIfExpired)
                .toList();
    }

    @Transactional
    public void finishExpiredVotings() {
        votingRepository.findByStatus(VotingStatus.ACTIVE).forEach(this::finishIfExpired);
    }

    @Transactional
    SecretVoting finishIfExpired(SecretVoting voting) {
        if (voting.getStatus() == VotingStatus.ACTIVE
                && voting.getEndsAt() != null
                && !voting.getEndsAt().isAfter(nowUtc())) {
            return completeVoting(getVotingForUpdate(voting.getId()), voting.getEndsAt());
        }
        return voting;
    }

    SecretVoting getVotingForUpdate(Long votingId) {
        return votingRepository.findWithLockById(votingId)
                .orElseThrow(() -> new ResourceNotFoundException("Voting was not found"));
    }

    public boolean canCurrentUserViewResults(SecretVoting voting) {
        return accessControl.hasRole(Role.ADMIN)
                || (accessControl.hasRole(Role.CURATOR) && accessControl.owns(voting.getCreatedBy()));
    }

    public void assertCanViewResults(SecretVoting voting) {
        accessControl.require(canCurrentUserViewResults(voting),
                "Current user cannot view results of this voting");
    }

    private SecretVoting completeVoting(SecretVoting voting, java.time.LocalDateTime finishedAt) {
        if (voting.getStatus() == VotingStatus.FINISHED) {
            return voting;
        }
        voting.setStatus(VotingStatus.FINISHED);
        voting.setFinishedAt(finishedAt);
        SecretVoting completed = votingRepository.save(voting);
        resultService.calculateAndSaveResults(completed);
        return completed;
    }

    private void assertCanViewVoting(SecretVoting voting) {
        Role role = accessControl.currentRole();
        boolean allowed = role == Role.ADMIN
                || (role == Role.CURATOR
                && (accessControl.owns(voting.getCreatedBy()) || accessControl.belongsToClass(voting.getSchoolClass())))
                || (role == Role.STUDENT && accessControl.currentProfile().isActive()
                && accessControl.belongsToClass(voting.getSchoolClass()));
        accessControl.require(allowed, "Current user cannot access this voting");
    }

    private void assertCanCreateVoting(Long classId) {
        Role role = accessControl.currentRole();
        boolean allowed = role == Role.ADMIN
                || (role == Role.CURATOR && accessControl.belongsToClass(classId));
        accessControl.require(allowed, "Current user cannot create voting for this class");
    }

    private void assertCanManageVoting(SecretVoting voting) {
        accessControl.require(
                accessControl.hasRole(Role.CURATOR) && accessControl.owns(voting.getCreatedBy()),
                "Current user cannot manage this voting"
        );
    }

    private void assertCanViewClassVotings(Long classId) {
        Role role = accessControl.currentRole();
        boolean allowed = role == Role.ADMIN
                || ((role == Role.STUDENT || role == Role.CURATOR)
                && accessControl.currentProfile().isActive()
                && accessControl.belongsToClass(classId));
        accessControl.require(allowed, "Current user cannot access votings for this class");
    }
}
