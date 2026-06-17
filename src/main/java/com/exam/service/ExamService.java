package com.exam.service;

import com.exam.dto.CreateExamRequest;
import com.exam.dto.ExamDashboardResponse;
import com.exam.dto.ExamDetailsResponse;
import com.exam.dto.GradeExamRequest;
import com.exam.model.Answer;
import com.exam.model.ExamAttempt;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.Question;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {

    private final ExamLifecycleService lifecycleService;
    private final ExamParticipationService participationService;
    private final ExamResultService resultService;

    public ExamService(
            ExamLifecycleService lifecycleService,
            ExamParticipationService participationService,
            ExamResultService resultService
    ) {
        this.lifecycleService = lifecycleService;
        this.participationService = participationService;
        this.resultService = resultService;
    }

    public ExamSession createExam(CreateExamRequest request) {
        return lifecycleService.createExam(request);
    }

    public ExamSession startSession(Long id) {
        return lifecycleService.startSession(id);
    }

    public ExamSession endSession(Long id) {
        return lifecycleService.endSession(id);
    }

    public ExamSession getSession(Long id) {
        return lifecycleService.getSession(id);
    }

    public List<ExamSession> getExams() {
        return lifecycleService.getExams();
    }

    public List<ExamSession> getExamsForCurrentUser() {
        return lifecycleService.getExamsForCurrentUser();
    }

    public List<ExamSession> getExamsForClass(Long classId) {
        return lifecycleService.getExamsForClass(classId);
    }

    public List<Question> getQuestions(Long sessionId) {
        return participationService.getQuestions(sessionId);
    }

    public Answer saveAnswer(Long sessionId, Long studentId, Long questionId, String text, boolean finalSubmitted) {
        return participationService.saveAnswer(sessionId, studentId, questionId, text, finalSubmitted);
    }

    public Answer saveCurrentUserAnswer(Long sessionId, Long questionId, String text, boolean finalSubmitted) {
        return participationService.saveCurrentUserAnswer(sessionId, questionId, text, finalSubmitted);
    }

    public ExamAttempt submitCurrentUserAttempt(Long sessionId) {
        return participationService.submitCurrentUserAttempt(sessionId);
    }

    public List<Answer> getAnswers(Long sessionId) {
        return participationService.getAnswers(sessionId);
    }

    public List<Answer> getStudentAnswers(Long sessionId, Long studentId) {
        return participationService.getStudentAnswers(sessionId, studentId);
    }

    public ExamDetailsResponse getDetails(Long sessionId) {
        return participationService.getDetails(sessionId);
    }

    public List<ExamResult> gradeExam(Long sessionId, GradeExamRequest request) {
        return resultService.gradeExam(sessionId, request);
    }

    public List<ExamResult> getResults(Long sessionId) {
        return resultService.getResults(sessionId);
    }

    public List<ExamResult> getStudentResults(Long studentId) {
        return resultService.getStudentResults(studentId);
    }

    public ExamDashboardResponse getDashboard() {
        return resultService.getDashboard();
    }

    public void finishExpiredExams() {
        lifecycleService.finishExpiredExams();
    }
}
