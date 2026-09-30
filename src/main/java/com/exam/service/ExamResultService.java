package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.ExamDashboardResponse;
import com.exam.dto.ExamSessionDTO;
import com.exam.dto.GradeExamRequest;
import com.exam.dto.StudentScoreRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import com.exam.model.User;
import com.exam.repository.ExamResultRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import com.exam.repository.ViolationRepository;
import com.exam.realtime.ExamRealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.exam.util.DateTimeUtils.nowUtc;

@Service
public class ExamResultService {

    private final ExamLifecycleService examLifecycleService;
    private final UserRepository userRepository;
    private final ExamResultRepository resultRepository;
    private final ViolationRepository violationRepository;
    private final SchoolClassRepository classRepository;
    private final ExamRealtimePublisher realtimePublisher;
    private final AccessControlService accessControl;
    private final SPointService sPointService;
    private final AdminDashboardService adminDashboardService;

    public ExamResultService(
            ExamLifecycleService examLifecycleService,
            UserRepository userRepository,
            ExamResultRepository resultRepository,
            ViolationRepository violationRepository,
            SchoolClassRepository classRepository,
            ExamRealtimePublisher realtimePublisher,
            AccessControlService accessControl,
            SPointService sPointService,
            AdminDashboardService adminDashboardService
    ) {
        this.examLifecycleService = examLifecycleService;
        this.userRepository = userRepository;
        this.resultRepository = resultRepository;
        this.violationRepository = violationRepository;
        this.classRepository = classRepository;
        this.realtimePublisher = realtimePublisher;
        this.accessControl = accessControl;
        this.sPointService = sPointService;
        this.adminDashboardService = adminDashboardService;
    }

    @Transactional
    public List<ExamResult> gradeExam(Long sessionId, GradeExamRequest request) {
        ExamSession session = examLifecycleService.getSession(sessionId);
        assertCanGrade(session);

        if (session.getStatus() != ExamStatus.FINISHED) {
            throw new BadRequestException("Exam must be finished before grading");
        }

        for (StudentScoreRequest scoreRequest : request.getScores()) {
            Optional<User> optionalStudent = userRepository.findById(scoreRequest.getStudentId());
            if (!optionalStudent.isPresent()) {
                throw new ResourceNotFoundException("Student was not found");
            }
            User student = optionalStudent.get();

            Optional<ExamResult> optionalResult = resultRepository.findBySessionIdAndStudentId(sessionId, student.getId());
            ExamResult result = optionalResult.isPresent() ? optionalResult.get() : new ExamResult();
            int violationPenalty = 0;
            for (com.exam.model.Violation violation : violationRepository.findBySession_IdAndUser_Id(sessionId, student.getId())) {
                if (violation.getPointsPenalty() != null) {
                    violationPenalty += violation.getPointsPenalty();
                }
            }
            result.setSession(session);
            result.setStudent(student);
            result.setRawScore(scoreRequest.getRawScore());
            result.setViolationPenalty(violationPenalty);
            result.setFinalScore(Math.max(0, scoreRequest.getRawScore() - violationPenalty));
            result.setGradedAt(nowUtc());
            resultRepository.save(result);
        }

        List<ExamResult> results = new ArrayList<>(resultRepository.findBySessionId(sessionId));
        results.sort(new Comparator<ExamResult>() {
            @Override
            public int compare(ExamResult first, ExamResult second) {
                return Integer.compare(second.getFinalScore(), first.getFinalScore());
            }
        });

        int place = 1;
        for (ExamResult result : results) {
            result.setRankPlace(place++);
            resultRepository.save(result);
        }

        sPointService.applyExamResults(session, results);
        adminDashboardService.publishDashboardUpdate();

        realtimePublisher.publish(sessionId, "EXAM_GRADED", "Exam results have been saved");
        return resultRepository.findBySessionId(sessionId);
    }

    public List<ExamResult> getResults(Long sessionId) {
        examLifecycleService.getSession(sessionId);
        return resultRepository.findBySessionId(sessionId);
    }

    public List<ExamResult> getStudentResults(Long studentId) {
        assertCanViewStudentResults(studentId);
        return resultRepository.findByStudentId(studentId);
    }

    public ExamDashboardResponse getDashboard() {
        List<ExamSessionDTO> exams = new ArrayList<>();
        for (ExamSession session : examLifecycleService.getExams()) {
            exams.add(ExamSessionDTO.from(session));
        }
        return new ExamDashboardResponse(exams, classRepository.findByActiveTrueOrderBySPointsDesc());
    }

    public List<ExamResult> getResultsForDetails(Long sessionId, Long studentId) {
        if (studentId == null) {
            return resultRepository.findBySessionId(sessionId);
        }
        Optional<ExamResult> optionalResult = resultRepository.findBySessionIdAndStudentId(sessionId, studentId);
        if (!optionalResult.isPresent()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(optionalResult.get());
    }

    private void assertCanGrade(ExamSession session) {
        accessControl.require(
                accessControl.hasRole(Role.EXAMINER) && accessControl.owns(session.getCreatedBy()),
                "Current user cannot grade this exam"
        );
    }

    private void assertCanViewStudentResults(Long studentId) {
        boolean allowed = accessControl.hasRole(Role.ADMIN) || accessControl.isCurrentProfile(studentId);
        accessControl.require(allowed, "Current user cannot access results for this student");
    }
}
