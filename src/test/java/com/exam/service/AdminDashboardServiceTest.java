package com.exam.service;

import com.exam.exception.ForbiddenException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.realtime.AdminDashboardRealtimePublisher;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"debug=false", "logging.level.root=warn"})
@ActiveProfiles("test")
@Transactional
@Import(AdminDashboardServiceTest.PublisherTrackingConfig.class)
class AdminDashboardServiceTest {

    @Autowired
    private AdminDashboardService dashboardService;
    @Autowired
    private SchoolClassRepository classRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClassRankPolicy classRankPolicy;
    @Autowired
    private TrackingAdminDashboardRealtimePublisher realtimePublisher;

    @BeforeEach
    void resetPublisher() {
        realtimePublisher.reset();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dashboardProvidesProposedRankFromCurrentSPoints() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        schoolClass.setsPoints(500);
        schoolClass.setRank(ClassRank.D);
        authenticateAs("admin");

        var dashboardClass = dashboardService.getDashboard().getClasses().stream()
                .filter(item -> item.getId().equals(schoolClass.getId()))
                .findFirst().orElseThrow();

        assertThat(dashboardClass.getProposedRank()).isEqualTo(ClassRank.A);
        assertThat(dashboardClass.isRankChangeRequired()).isTrue();
    }

    @Test
    void confirmsOnlySelectedClass() {
        SchoolClass selected = classRepository.findByName("10A").orElseThrow();
        SchoolClass untouched = classRepository.findByName("10B").orElseThrow();
        selected.setsPoints(500);
        selected.setRank(ClassRank.D);
        untouched.setsPoints(500);
        untouched.setRank(ClassRank.D);
        authenticateAs("admin");

        dashboardService.confirmRankUpdate(selected.getId());

        assertThat(selected.getRank()).isEqualTo(ClassRank.A);
        assertThat(untouched.getRank()).isEqualTo(ClassRank.D);
        assertThat(realtimePublisher.publishCount()).isEqualTo(1);
    }

    @Test
    void leavesRankUntouchedWhenItAlreadyMatchesProposal() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        schoolClass.setsPoints(300);
        schoolClass.setRank(ClassRank.B);
        authenticateAs("admin");

        dashboardService.confirmRankUpdate(schoolClass.getId());

        assertThat(schoolClass.getRank()).isEqualTo(ClassRank.B);
    }

    @Test
    void rejectsMissingClassAndNonAdminAccess() {
        authenticateAs("admin");
        assertThatThrownBy(() -> dashboardService.getRankDetails(Long.MAX_VALUE))
                .isInstanceOf(ResourceNotFoundException.class);

        authenticateAs("student");
        assertThatThrownBy(() -> dashboardService.getDashboard())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void rankDetailsExcludeInactiveStudents() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        List<User> students = userRepository.findBySchoolClassIdAndActiveTrue(schoolClass.getId());
        User inactiveStudent = students.getFirst();
        inactiveStudent.setActive(false);
        authenticateAs("admin");

        var details = dashboardService.getRankDetails(schoolClass.getId());

        assertThat(details.students()).extracting("studentId").doesNotContain(inactiveStudent.getId());
    }

    @Test
    void rankPolicyResolvesAllRanksAndExposesNextThreshold() {
        assertThat(classRankPolicy.resolve(0)).isEqualTo(ClassRank.D);
        assertThat(classRankPolicy.resolve(120)).isEqualTo(ClassRank.C);
        assertThat(classRankPolicy.resolve(300)).isEqualTo(ClassRank.B);
        assertThat(classRankPolicy.resolve(500)).isEqualTo(ClassRank.A);
        assertThat(classRankPolicy.minimumPointsFor(ClassRank.D)).isZero();
        assertThat(classRankPolicy.minimumPointsFor(ClassRank.C)).isEqualTo(120);
        assertThat(classRankPolicy.minimumPointsFor(ClassRank.B)).isEqualTo(300);
        assertThat(classRankPolicy.minimumPointsFor(ClassRank.A)).isEqualTo(500);
        assertThat(classRankPolicy.nextHigherRank(ClassRank.D)).isEqualTo(ClassRank.C);
        assertThat(classRankPolicy.nextHigherRank(ClassRank.C)).isEqualTo(ClassRank.B);
        assertThat(classRankPolicy.nextHigherRank(ClassRank.B)).isEqualTo(ClassRank.A);
        assertThat(classRankPolicy.nextHigherRank(ClassRank.A)).isNull();
    }

    @Test
    void rankDetailsExposeStoredAndCalculatedSPointTotals() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        schoolClass.setsPoints(350);
        authenticateAs("admin");

        var details = dashboardService.getRankDetails(schoolClass.getId());
        int expectedStudentsTotal = userRepository.findBySchoolClassIdAndActiveTrue(schoolClass.getId()).stream()
                .mapToInt(User::getsPoints)
                .sum();

        assertThat(details.totalSPoints()).isEqualTo(350);
        assertThat(details.calculatedStudentsSPoints()).isEqualTo(expectedStudentsTotal);
        assertThat(details.nextHigherRank()).isEqualTo(ClassRank.A);
        assertThat(details.pointsToNextHigherRank()).isEqualTo(150);
    }

    @Test
    void nonAdminCannotConfirmOrReadRankDetails() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        authenticateAs("student");

        assertThatThrownBy(() -> dashboardService.confirmRankUpdate(schoolClass.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> dashboardService.getRankDetails(schoolClass.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }

    @TestConfiguration
    static class PublisherTrackingConfig {
        @Bean
        @Primary
        TrackingAdminDashboardRealtimePublisher trackingRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            return new TrackingAdminDashboardRealtimePublisher(messagingTemplate);
        }
    }

    static class TrackingAdminDashboardRealtimePublisher extends AdminDashboardRealtimePublisher {
        private int publishCount;

        TrackingAdminDashboardRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            super(messagingTemplate);
        }

        @Override
        public void publish(com.exam.dto.AdminDashboardResponse response) {
            publishCount++;
        }

        void reset() {
            publishCount = 0;
        }

        int publishCount() {
            return publishCount;
        }
    }
}
