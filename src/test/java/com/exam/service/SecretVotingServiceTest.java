package com.exam.service;

import com.exam.dto.CreateVotingRequest;
import com.exam.dto.VotingOptionRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ForbiddenException;
import com.exam.model.SchoolClass;
import com.exam.model.SecretVoting;
import com.exam.model.Vote;
import com.exam.model.VotingOption;
import com.exam.model.VotingStatus;
import com.exam.repository.VotingResultRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.SecretVotingRepository;
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
    private final VotingResultRepository resultRepository;

    @Autowired
    SecretVotingServiceTest(
            VotingService votingService,
            SchoolClassRepository classRepository,
            SecretVotingRepository votingRepository,
            VotingResultRepository resultRepository
    ) {
        this.votingService = votingService;
        this.classRepository = classRepository;
        this.votingRepository = votingRepository;
        this.resultRepository = resultRepository;
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
    void rejectsVotingWithFewerThanTwoOptions() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        CreateVotingRequest request = request(ownClass.getId(), nowUtc().plusMinutes(30));
        request.setOptions(List.of(option("Only option")));

        assertThatThrownBy(() -> votingService.createVoting(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void studentVoteIsRecordedAnonymouslyAndCounted() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));
        VotingOption option = votingService.getOptions(voting.getId()).get(0);
        authenticateAs("student");
        Vote vote = votingService.submitCurrentUserVote(voting.getId(), option.getId());

        assertThat(vote.getVotingId()).isEqualTo(voting.getId());
        assertThat(vote.getOption().getId()).isEqualTo(option.getId());
        // Vote has no voter field; the only student association is kept in
        // VotingReceipt and is never linked back to the Vote.

        authenticateAs("curator");
        votingService.finishVoting(voting.getId());
        Map<String, Long> results = votingService.getResults(voting.getId());
        assertThat(results.get(option.getLabel())).isEqualTo(1L);
        assertThat(resultRepository.findByVotingIdOrderByOptionId(voting.getId()))
                .hasSize(2)
                .extracting(result -> result.getVotesCount())
                .contains(1L, 0L);
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
    void studentFromAnotherClassAndNonStudentCannotVote() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));
        VotingOption option = votingService.getOptions(voting.getId()).getFirst();

        authenticateAs("hirata");
        assertThatThrownBy(() -> votingService.submitCurrentUserVote(voting.getId(), option.getId()))
                .isInstanceOf(BadRequestException.class);

        authenticateAs("examiner");
        assertThatThrownBy(() -> votingService.submitCurrentUserVote(voting.getId(), option.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reopeningVotingKeepsHasVotedState() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));
        VotingOption option = votingService.getOptions(voting.getId()).getFirst();

        authenticateAs("student");
        votingService.submitCurrentUserVote(voting.getId(), option.getId());

        assertThat(votingService.getDetails(voting.getId()).isHasVoted()).isTrue();
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

        SecretVoting finished = votingRepository.findById(voting.getId()).orElseThrow();
        assertThat(finished.getStatus()).isEqualTo(VotingStatus.FINISHED);
        assertThat(finished.getFinishedAt()).isEqualTo(finished.getEndsAt());
        assertThat(resultRepository.findByVotingIdOrderByOptionId(voting.getId())).hasSize(2);
    }

    @Test
    void repeatedFinishKeepsOriginalTimestampAndDoesNotDuplicateResults() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));

        votingService.finishVoting(voting.getId());
        var originalFinishedAt = votingRepository.findById(voting.getId()).orElseThrow().getFinishedAt();
        votingService.finishVoting(voting.getId());

        SecretVoting stored = votingRepository.findById(voting.getId()).orElseThrow();
        assertThat(stored.getFinishedAt()).isEqualTo(originalFinishedAt);
        assertThat(resultRepository.findByVotingIdOrderByOptionId(voting.getId())).hasSize(2);
    }

    @Test
    void finishedVotingRejectsNewVotesAndReturnsStoredResults() {
        SchoolClass ownClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("curator");
        SecretVoting voting = votingService.createVoting(request(ownClass.getId(), nowUtc().plusMinutes(30)));
        VotingOption option = votingService.getOptions(voting.getId()).getFirst();
        votingService.finishVoting(voting.getId());

        authenticateAs("student");
        assertThatThrownBy(() -> votingService.submitCurrentUserVote(voting.getId(), option.getId()))
                .isInstanceOf(BadRequestException.class);

        authenticateAs("curator");
        assertThat(votingService.getResults(voting.getId())).containsEntry(option.getLabel(), 0L);
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
