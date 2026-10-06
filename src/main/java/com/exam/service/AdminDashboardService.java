package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.AdminDashboardClassResponse;
import com.exam.dto.AdminDashboardResponse;
import com.exam.dto.ConfirmRankUpdatesRequest;
import com.exam.dto.RankDetailsResponse;
import com.exam.dto.RankDetailsStudentResponse;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;
import com.exam.realtime.AdminDashboardRealtimePublisher;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardService {

    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final ClassRankPolicy classRankPolicy;
    private final AccessControlService accessControl;
    private final AdminDashboardRealtimePublisher realtimePublisher;

    public AdminDashboardService(
            SchoolClassRepository classRepository,
            UserRepository userRepository,
            ClassRankPolicy classRankPolicy,
            AccessControlService accessControl,
            AdminDashboardRealtimePublisher realtimePublisher
    ) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.classRankPolicy = classRankPolicy;
        this.accessControl = accessControl;
        this.realtimePublisher = realtimePublisher;
    }

    public AdminDashboardResponse getDashboard() {
        assertAdmin();
        return buildDashboardResponse();
    }

    public AdminDashboardResponse getRankPreview() {
        assertAdmin();
        return buildDashboardResponse();
    }

    @Transactional
    public AdminDashboardResponse confirmRankUpdates(ConfirmRankUpdatesRequest request) {
        assertAdmin();

        for (SchoolClass schoolClass : classRepository.findAllById(request.getClassIds())) {
            ClassRank proposedRank = classRankPolicy.resolve(schoolClass.getsPoints());
            if (proposedRank != schoolClass.getRank()) {
                schoolClass.setRank(proposedRank);
                classRepository.save(schoolClass);
            }
        }

        AdminDashboardResponse response = buildDashboardResponse();
        realtimePublisher.publish(response);
        return response;
    }

    @Transactional
    public AdminDashboardResponse confirmRankUpdate(Long classId) {
        assertAdmin();
        SchoolClass schoolClass = findClass(classId);
        ClassRank proposedRank = classRankPolicy.resolve(schoolClass.getsPoints());
        if (proposedRank != schoolClass.getRank()) {
            schoolClass.setRank(proposedRank);
            classRepository.save(schoolClass);
        }

        AdminDashboardResponse response = buildDashboardResponse();
        realtimePublisher.publish(response);
        return response;
    }

    public RankDetailsResponse getRankDetails(Long classId) {
        assertAdmin();
        SchoolClass schoolClass = findClass(classId);
        ClassRank proposedRank = classRankPolicy.resolve(schoolClass.getsPoints());
        ClassRank nextHigherRank = classRankPolicy.nextHigherRank(proposedRank);
        Integer pointsToNextHigherRank = nextHigherRank == null
                ? null
                : Math.max(0, classRankPolicy.minimumPointsFor(nextHigherRank) - schoolClass.getsPoints());
        List<RankDetailsStudentResponse> students = userRepository.findBySchoolClassIdAndActiveTrue(schoolClass.getId())
                .stream()
                .map(RankDetailsStudentResponse::from)
                .toList();

        return new RankDetailsResponse(
                schoolClass.getId(), schoolClass.getName(), schoolClass.getRank(), proposedRank,
                schoolClass.getsPoints(), proposedRank != schoolClass.getRank(),
                classRankPolicy.minimumPointsFor(proposedRank), nextHigherRank, pointsToNextHigherRank, students
        );
    }

    public void publishDashboardUpdate() {
        realtimePublisher.publish(buildDashboardResponse());
    }

    private AdminDashboardResponse buildDashboardResponse() {
        List<AdminDashboardClassResponse> classes = new ArrayList<>();
        List<SchoolClass> schoolClasses = classRepository.findByActiveTrueOrderBySPointsDesc();
        for (SchoolClass schoolClass : schoolClasses) {
            classes.add(toDashboardClass(schoolClass));
        }
        return new AdminDashboardResponse(classes);
    }

    private AdminDashboardClassResponse toDashboardClass(SchoolClass schoolClass) {
        long studentCount = 0;
        for (com.exam.model.User student : userRepository.findBySchoolClassIdAndActiveTrue(schoolClass.getId())) {
            studentCount++;
        }
        return AdminDashboardClassResponse.from(
                schoolClass,
                classRankPolicy.resolve(schoolClass.getsPoints()),
                studentCount
        );
    }

    private SchoolClass findClass(Long classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("School class not found: " + classId));
    }

    private void assertAdmin() {
        accessControl.require(accessControl.hasRole(Role.ADMIN), "Current user cannot access admin dashboard");
    }
}
