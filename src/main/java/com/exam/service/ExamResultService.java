package com.exam.service;

import com.exam.dto.ExamDashboardResponse;
import com.exam.dto.ExamSessionDTO;
import com.exam.dto.GradeExamRequest;
import com.exam.dto.StudentScoreRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.ExamResultRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import com.exam.repository.ViolationRepository;
import com.exam.realtime.ExamRealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ExamResultService {

    private final ExamLifecycleService examLifecycleService;
    private final UserRepository userRepository;
    private final ExamResultRepository resultRepository;
    private final ViolationRepository violationRepository;
    private final SchoolClassRepository classRepository;
    private final ExamRealtimePublisher realtimePublisher;

    public ExamResultService(
            ExamLifecycleService examLifecycleService,
            UserRepository userRepository,
            ExamResultRepository resultRepository,
            ViolationRepository violationRepository,
            SchoolClassRepository classRepository,
            ExamRealtimePublisher realtimePublisher
    ) {
        this.examLifecycleService = examLifecycleService;
        this.userRepository = userRepository;
        this.resultRepository = resultRepository;
        this.violationRepository = violationRepository;
        this.classRepository = classRepository;
        this.realtimePublisher = realtimePublisher;
    }

    @Transactional
    public List<ExamResult> gradeExam(Long sessionId, GradeExamRequest request) {
        ExamSession session = examLifecycleService.getSession(sessionId);

        if (session.getStatus() != ExamStatus.FINISHED) {
            throw new BadRequestException("Exam must be finished before grading");
        }

        for (StudentScoreRequest scoreRequest : request.getScores()) {
            User student = userRepository.findById(scoreRequest.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student was not found"));

            ExamResult result = resultRepository.findBySessionIdAndStudentId(sessionId, student.getId())
                    .orElseGet(ExamResult::new);
            int violationPenalty = violationRepository.findBySessionIdAndUserId(sessionId, student.getId()).stream()
                    .mapToInt(violation -> violation.getPointsPenalty() == null ? 0 : violation.getPointsPenalty())
                    .sum();
            result.setSession(session);
            result.setStudent(student);
            result.setRawScore(scoreRequest.getRawScore());
            result.setViolationPenalty(violationPenalty);
            result.setFinalScore(Math.max(0, scoreRequest.getRawScore() - violationPenalty));
            result.setGradedAt(LocalDateTime.now());
            resultRepository.save(result);
        }

        List<ExamResult> results = resultRepository.findBySessionId(sessionId).stream()
                .sorted(Comparator.comparingInt(ExamResult::getFinalScore).reversed())
                .toList();

        int place = 1;
        int totalScore = 0;
        for (ExamResult result : results) {
            result.setRankPlace(place++);
            totalScore += result.getFinalScore();
            resultRepository.save(result);
        }

        if (session.getSchoolClass() != null && !results.isEmpty()) {
            SchoolClass schoolClass = session.getSchoolClass();
            int averageScore = totalScore / results.size();
            schoolClass.setsPoints(schoolClass.getsPoints() + averageScore);
            classRepository.save(schoolClass);
        }

        realtimePublisher.publish(sessionId, "EXAM_GRADED", "Exam results have been saved");
        return resultRepository.findBySessionId(sessionId);
    }

    public List<ExamResult> getResults(Long sessionId) {
        examLifecycleService.getSession(sessionId);
        return resultRepository.findBySessionId(sessionId);
    }

    public List<ExamResult> getStudentResults(Long studentId) {
        return resultRepository.findByStudentId(studentId);
    }

    public ExamDashboardResponse getDashboard() {
        return new ExamDashboardResponse(
                examLifecycleService.getExams().stream().map(ExamSessionDTO::from).toList(),
                classRepository.findByActiveTrueOrderBySPointsDesc()
        );
    }

    public List<ExamResult> getResultsForDetails(Long sessionId, Long studentId) {
        if (studentId == null) {
            return resultRepository.findBySessionId(sessionId);
        }
        return resultRepository.findBySessionIdAndStudentId(sessionId, studentId)
                .map(List::of)
                .orElseGet(List::of);
    }
}
