package com.exam.service;

import com.exam.exception.ForbiddenException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class AdminDashboardServiceTest {

    @Autowired
    private AdminDashboardService dashboardService;
    @Autowired
    private SchoolClassRepository classRepository;
    @Autowired
    private UserRepository userRepository;

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

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of())
        );
    }
}
