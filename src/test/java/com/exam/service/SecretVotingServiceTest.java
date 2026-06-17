package com.exam.service;

import com.exam.auth.AppUserRepository;
import com.exam.dto.CreateVotingRequest;
import com.exam.dto.VotingOptionRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ForbiddenException;
import com.exam.model.SchoolClass;
import com.exam.model.SecretVoting;
import com.exam.model.Vote;
import com.exam.model.VotingOption;
import com.exam.model.VotingStatus;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.SecretVotingRepository;
import com.exam.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.exam.util.DateTimeUtils.nowUtc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        properties = {
                "debug=false",
                "logging.level.root=warn",
                "logging.level.com.exam=info",
                "logging.level.org.springframework=warn"
        }
)
@ActiveProfiles("test")
class SecretVotingServiceTest {

    private final VotingService votingService;
    private final SchoolClassRepository classRepository;
    private final SecretVotingRepository votingRepository;
    private final UserRepository userRepository;
    private final AppUserRepository accountRepository;

    @Autowired
    SecretVotingServiceTest(
            VotingService votingService,
            SchoolClassRepository classRepository,
            SecretVotingRepository votingRepository,
            UserRepository userRepository,
            AppUserRepository accountRepository
    ) {
        this.votingService = votingService;
        this.classRepository = classRepository;
        this.votingRepository = votingRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void curatorCreatesVotingForOwnClass() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");

        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));

        assertThat(voting.getId()).isNotNull();
        assertThat(voting.getStatus()).isEqualTo(VotingStatus.ACTIVE);
        assertThat(voting.getSchoolClass().getId()).isEqualTo(ownClass.getId());
        assertThat(votingService.getOptions(voting.getId())).hasSize(2);
    }

    @Test
    void curatorCannotCreateVotingForAnotherClass() {
        SchoolClass foreignClass = classRepository.findByName("10B").orElseThrow();
        authenticateAs("curator");

        assertThatThrownBy(() -> votingService.createVoting(request(foreignClass.getId(), nowUtc().plusMinutes(30))))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rejectsVotingWithoutEndTime() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");

        assertThatThrownBy(() -> votingService.createVoting(request(ownClass.getId(), null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsVotingWithPastEndTime() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");

        assertThatThrownBy(() -> votingService.createVoting(request(ownClass.getId(), nowUtc().minusMinutes(1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void studentVoteIsRecordedAnonymouslyAndCounted() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));
        VotingOption option = votingService.getOptions(voting.getId()).get(0);
        Long studentAccountId = accountRepository.findByUsername("student").orElseThrow().getId();
        Long studentId = userRepository.findByAccountId(studentAccountId).orElseThrow().getId();

        authenticateAs("student");
        Vote vote = votingService.submitCurrentUserVote(voting.getId(), option.getId());

        assertThat(vote.getVotingId()).isEqualTo(voting.getId());
        assertThat(vote.getEncryptedValue()).isNotBlank();
        assertThat(vote.getAnonymousVoterHash())
                .isNotEqualTo(String.valueOf(studentId))
                .isNotEqualTo(voting.getId() + ":" + studentId);

        Map<String, Long> results = votingService.getResults(voting.getId());
        assertThat(results.get(option.getLabel())).isEqualTo(1L);
    }

    @Test
    void studentCannotVoteTwice() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));
        VotingOption option = votingService.getOptions(voting.getId()).get(0);

        authenticateAs("student");
        votingService.submitCurrentUserVote(voting.getId(), option.getId());

        assertThatThrownBy(() -> votingService.submitCurrentUserVote(voting.getId(), option.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void expiredVotingIsFinishedAutomatically() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));

        SecretVoting stored = votingRepository.findById(voting.getId()).orElseThrow();
        stored.setEndsAt(nowUtc().minusMinutes(1));
        votingRepository.save(stored);

        votingService.finishExpiredVotings();

        assertThat(votingRepository.findById(voting.getId()).orElseThrow().getStatus())
                .isEqualTo(VotingStatus.FINISHED);
    }

    private CreateVotingRequest request(Long classId, LocalDateTime endsAt) {
        CreateVotingRequest request = new CreateVotingRequest();
        request.setTitle("Class representative");
        request.setDescription("Pick a candidate");
        request.setClassId(classId);
        request.setEndsAt(endsAt);
        request.setOptions(List.of(option("Yuki"), option("Horikita")));
        return request;
    }

    private VotingOptionRequest option(String label) {
        VotingOptionRequest option = new VotingOptionRequest();
        option.setLabel(label);
        return option;
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }
}
