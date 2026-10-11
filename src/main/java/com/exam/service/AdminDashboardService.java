package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.AdminDashboardClassResponse;
import com.exam.dto.AdminDashboardResponse;
import com.exam.dto.ConfirmRankUpdatesRequest;
import com.exam.dto.RankDetailsResponse;
import com.exam.dto.RankDetailsStudentResponse;
import com.exam.dto.StudentSPointHistoryResponse;
import com.exam.dto.StudentExamResultResponse;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.SPointTransactionRepository;
import com.exam.repository.ExamResultRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.UserRepository;
import com.exam.event.ClassRankUpdatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final SPointTransactionRepository sPointTransactionRepository;
    private final ExamResultRepository examResultRepository;
    private final QuestionRepository questionRepository;
    private final ClassRankPolicy classRankPolicy;
    private final AccessControlService accessControl;
    private final ApplicationEventPublisher eventPublisher;

    public AdminDashboardService(
            SchoolClassRepository classRepository,
            UserRepository userRepository,
            SPointTransactionRepository sPointTransactionRepository,
            ExamResultRepository examResultRepository,
            QuestionRepository questionRepository,
            ClassRankPolicy classRankPolicy,
            AccessControlService accessControl,
            ApplicationEventPublisher eventPublisher
    ) {
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.sPointTransactionRepository = sPointTransactionRepository;
        this.examResultRepository = examResultRepository;
        this.questionRepository = questionRepository;
        this.classRankPolicy = classRankPolicy;
        this.accessControl = accessControl;
        this.eventPublisher = eventPublisher;
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

        eventPublisher.publishEvent(new ClassRankUpdatedEvent(request.getClassIds()));
        return buildDashboardResponse();
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

        eventPublisher.publishEvent(new ClassRankUpdatedEvent(List.of(classId)));
        return buildDashboardResponse();
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
        int calculatedStudentsSPoints = students.stream()
                .mapToInt(RankDetailsStudentResponse::sPoints)
                .sum();

        return new RankDetailsResponse(
                schoolClass.getId(), schoolClass.getName(), schoolClass.getRank(), proposedRank,
                schoolClass.getsPoints(), proposedRank != schoolClass.getRank(),
                classRankPolicy.minimumPointsFor(proposedRank), nextHigherRank, pointsToNextHigherRank,
                calculatedStudentsSPoints, students
        );
    }

    @Transactional(readOnly = true)
    public StudentSPointHistoryResponse getStudentSPointHistory(Long classId, Long studentId) {
        assertAdmin();
        findClass(classId);
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));
        if (student.getSchoolClass() == null || !classId.equals(student.getSchoolClass().getId())) {
            throw new ResourceNotFoundException("Student does not belong to school class: " + classId);
        }

        List<com.exam.model.ExamResult> examResults = examResultRepository
                .findByStudentIdAndSessionSchoolClassId(studentId, classId);
        Map<Long, Integer> maxScores = findMaxScores(examResults);
        List<StudentExamResultResponse> resultResponses = examResults.stream()
                .map(result -> StudentExamResultResponse.from(result, maxScores.getOrDefault(result.getSession().getId(), 0)))
                .toList();

        return new StudentSPointHistoryResponse(
                student.getId(),
                student.getsPoints(),
                resultResponses.stream().mapToInt(StudentExamResultResponse::finalScore).sum(),
                resultResponses,
                sPointTransactionRepository.findHistoryByUserIdAndClassId(studentId, classId)
                        .stream()
                        .map(com.exam.dto.SPointTransactionResponse::from)
                        .toList()
        );
    }

    private Map<Long, Integer> findMaxScores(List<com.exam.model.ExamResult> examResults) {
        if (examResults.isEmpty()) {
            return Map.of();
        }
        List<Long> sessionIds = examResults.stream().map(result -> result.getSession().getId()).toList();
        Map<Long, Integer> maxScores = new HashMap<>();
        for (Object[] row : questionRepository.sumMaxScoreBySessionIds(sessionIds)) {
            maxScores.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return maxScores;
    }

    /**
     * Provides a fresh snapshot for infrastructure listeners after a transaction commits.
     * This method is invoked by a separate Spring bean, so the transactional proxy opens
     * an independent read transaction after the originating write has committed.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public AdminDashboardResponse getDashboardSnapshot() {
        return buildDashboardResponse();
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
