package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.AdminDashboardClassResponse;
import com.exam.dto.AdminDashboardResponse;
import com.exam.dto.ConfirmRankUpdatesRequest;
import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;
import com.exam.realtime.AdminDashboardRealtimePublisher;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public void publishDashboardUpdate() {
        realtimePublisher.publish(buildDashboardResponse());
    }

    private AdminDashboardResponse buildDashboardResponse() {
        List<AdminDashboardClassResponse> classes = classRepository.findByActiveTrueOrderBySPointsDesc().stream()
                .map(this::toDashboardClass)
                .toList();
        return new AdminDashboardResponse(classes);
    }

    private AdminDashboardClassResponse toDashboardClass(SchoolClass schoolClass) {
        long studentCount = userRepository.findBySchoolClassIdAndActiveTrue(schoolClass.getId()).stream().count();
        return AdminDashboardClassResponse.from(
                schoolClass,
                classRankPolicy.resolve(schoolClass.getsPoints()),
                studentCount
        );
    }

    private void assertAdmin() {
        accessControl.require(accessControl.hasRole(Role.ADMIN), "Current user cannot access admin dashboard");
    }
}
