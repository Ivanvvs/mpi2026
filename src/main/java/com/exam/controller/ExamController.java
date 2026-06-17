package com.exam.controller;

import com.exam.dto.AnswerDTO;
import com.exam.dto.CreateExamRequest;
import com.exam.dto.ExamDashboardResponse;
import com.exam.dto.ExamDetailsResponse;
import com.exam.dto.ExamResultResponse;
import com.exam.dto.ExamSessionDTO;
import com.exam.dto.GradeExamRequest;
import com.exam.dto.QuestionResponse;
import com.exam.dto.SubmitAnswerRequest;
import com.exam.dto.SubmitOwnAnswerRequest;
import com.exam.model.ExamAttempt;
import com.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/exam/session")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<ExamSessionDTO> list() {
        return examService.getExamsForCurrentUser().stream().map(ExamSessionDTO::from).toList();
    }

    @GetMapping("/my")
    public List<ExamSessionDTO> myExams() {
        return examService.getExamsForCurrentUser().stream().map(ExamSessionDTO::from).toList();
    }

    @GetMapping("/dashboard")
    public ExamDashboardResponse dashboard() {
        return examService.getDashboard();
    }

    @PostMapping
    public ExamSessionDTO createFromRequest(@Valid @RequestBody CreateExamRequest request) {
        return ExamSessionDTO.from(examService.createExam(request));
    }

    @PostMapping("/start/{id}")
    public ExamSessionDTO start(@PathVariable Long id) {
        return ExamSessionDTO.from(examService.startSession(id));
    }

    @PostMapping("/end/{id}")
    public ExamSessionDTO end(@PathVariable Long id) {
        return ExamSessionDTO.from(examService.endSession(id));
    }

    @GetMapping("/{id}")
    public ExamSessionDTO get(@PathVariable Long id) {
        return ExamSessionDTO.from(examService.getSession(id));
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
    public List<QuestionResponse> questions(@PathVariable Long id) {
        return examService.getQuestions(id).stream()
                .map(question -> QuestionResponse.from(question, true))
                .toList();
    }

    @PostMapping("/{id}/answers")
    public AnswerDTO saveAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return AnswerDTO.from(examService.saveAnswer(
                id,
                request.getStudentId(),
                request.getQuestionId(),
                request.getText(),
                request.isFinalSubmitted()
        ));
    }

    @PostMapping("/{id}/answers/me")
    public AnswerDTO saveMyAnswer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitOwnAnswerRequest request
    ) {
        return AnswerDTO.from(examService.saveCurrentUserAnswer(
                id,
                request.getQuestionId(),
                request.getText(),
                request.isFinalSubmitted()
        ));
    }

    @PostMapping("/{id}/attempt/me/submit")
    public ExamAttempt submitMyAttempt(@PathVariable Long id) {
        return examService.submitCurrentUserAttempt(id);
    }

    @GetMapping("/{id}/answers")
    public List<AnswerDTO> answers(@PathVariable Long id) {
        return examService.getAnswers(id).stream().map(AnswerDTO::from).toList();
    }

    @GetMapping("/{id}/answers/{studentId}")
    public List<AnswerDTO> studentAnswers(@PathVariable Long id, @PathVariable Long studentId) {
        return examService.getStudentAnswers(id, studentId).stream().map(AnswerDTO::from).toList();
    }

    @PostMapping("/{id}/grades")
    public List<ExamResultResponse> grade(
            @PathVariable Long id,
            @Valid @RequestBody GradeExamRequest request
    ) {
        return examService.gradeExam(id, request).stream().map(ExamResultResponse::from).toList();
    }

    @GetMapping("/{id}/results")
    public List<ExamResultResponse> results(@PathVariable Long id) {
        return examService.getResults(id).stream().map(ExamResultResponse::from).toList();
    }

    @GetMapping("/students/{studentId}/results")
    public List<ExamResultResponse> studentResults(@PathVariable Long studentId) {
        return examService.getStudentResults(studentId).stream().map(ExamResultResponse::from).toList();
    }
}
