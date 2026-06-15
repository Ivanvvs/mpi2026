package com.exam.controller;

import com.exam.dto.CreateExamRequest;
import com.exam.dto.ExamDashboardResponse;
import com.exam.dto.ExamDetailsResponse;
import com.exam.dto.GradeExamRequest;
import com.exam.dto.SubmitAnswerRequest;
import com.exam.dto.SubmitOwnAnswerRequest;
import com.exam.model.Answer;
import com.exam.model.ExamAttempt;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.Question;
import com.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam/session")
@CrossOrigin(origins = "*")
public class ExamController {

    @Autowired
    private ExamService examService;

    @PostMapping("/create")
    public ExamSession create(@RequestBody ExamSession session) {
        return examService.createSession(session);
    }

    @GetMapping
    public List<ExamSession> list() {
        return examService.getExams();
    }

    @GetMapping("/my")
    public List<ExamSession> myExams() {
        return examService.getExamsForCurrentUser();
    }

    @GetMapping("/dashboard")
    public ExamDashboardResponse dashboard() {
        return examService.getDashboard();
    }

    @PostMapping
    public ExamSession createFromRequest(@Valid @RequestBody CreateExamRequest request) {
        return examService.createExam(request);
    }

    @PostMapping("/start/{id}")
    public ExamSession start(@PathVariable Long id) {
        return examService.startSession(id);
    }

    @PostMapping("/end/{id}")
    public ExamSession end(@PathVariable Long id) {
        return examService.endSession(id);
    }

    @GetMapping("/{id}")
    public ExamSession get(@PathVariable Long id) {
        return examService.getSession(id);
    }

    @GetMapping("/{id}/details")
    public ExamDetailsResponse details(@PathVariable Long id) {
        return examService.getDetails(id);
    }

    @GetMapping("/{id}/monitor")
    public ExamDetailsResponse monitor(@PathVariable Long id) {
        return examService.getDetails(id);
    }

    @GetMapping("/{id}/questions")
    public List<Question> questions(@PathVariable Long id) {
        return examService.getQuestions(id);
    }

    @PostMapping("/{id}/answers")
    public Answer saveAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return examService.saveAnswer(
                id,
                request.getStudentId(),
                request.getQuestionId(),
                request.getText(),
                request.isFinalSubmitted()
        );
    }

    @PostMapping("/{id}/answers/me")
    public Answer saveMyAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitOwnAnswerRequest request
    ) {
        return examService.saveCurrentUserAnswer(
                id,
                request.getQuestionId(),
                request.getText(),
                request.isFinalSubmitted()
        );
    }

    @PostMapping("/{id}/attempt/me/submit")
    public ExamAttempt submitMyAttempt(@PathVariable Long id) {
        return examService.submitCurrentUserAttempt(id);
    }

    @GetMapping("/{id}/answers")
    public List<Answer> answers(@PathVariable Long id) {
        return examService.getAnswers(id);
    }

    @GetMapping("/{id}/answers/{studentId}")
    public List<Answer> studentAnswers(@PathVariable Long id, @PathVariable Long studentId) {
        return examService.getStudentAnswers(id, studentId);
    }

    @PostMapping("/{id}/grades")
    public List<ExamResult> grade(
            @PathVariable Long id,
            @Valid @RequestBody GradeExamRequest request
    ) {
        return examService.gradeExam(id, request);
    }

    @GetMapping("/{id}/results")
    public List<ExamResult> results(@PathVariable Long id) {
        return examService.getResults(id);
    }

    @GetMapping("/students/{studentId}/results")
    public List<ExamResult> studentResults(@PathVariable Long studentId) {
        return examService.getStudentResults(studentId);
    }
}
