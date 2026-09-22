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
import com.exam.model.Answer;
import com.exam.model.ExamAttempt;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.Question;
import com.exam.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
        List<ExamSessionDTO> responses = new ArrayList<>();
        for (ExamSession session : examService.getExamsForCurrentUser()) {
            responses.add(ExamSessionDTO.from(session));
        }
        return responses;
    }

    @GetMapping("/my")
    public List<ExamSessionDTO> myExams() {
        List<ExamSessionDTO> responses = new ArrayList<>();
        for (ExamSession session : examService.getExamsForCurrentUser()) {
            responses.add(ExamSessionDTO.from(session));
        }
        return responses;
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
        List<QuestionResponse> responses = new ArrayList<>();
        for (Question question : examService.getQuestions(id)) {
            responses.add(QuestionResponse.from(question, true));
        }
        return responses;
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
        List<AnswerDTO> responses = new ArrayList<>();
        for (Answer answer : examService.getAnswers(id)) {
            responses.add(AnswerDTO.from(answer));
        }
        return responses;
    }

    @GetMapping("/{id}/answers/{studentId}")
    public List<AnswerDTO> studentAnswers(@PathVariable Long id, @PathVariable Long studentId) {
        List<AnswerDTO> responses = new ArrayList<>();
        for (Answer answer : examService.getStudentAnswers(id, studentId)) {
            responses.add(AnswerDTO.from(answer));
        }
        return responses;
    }

    @PostMapping("/{id}/grades")
    public List<ExamResultResponse> grade(
            @PathVariable Long id,
            @Valid @RequestBody GradeExamRequest request
    ) {
        List<ExamResultResponse> responses = new ArrayList<>();
        for (ExamResult result : examService.gradeExam(id, request)) {
            responses.add(ExamResultResponse.from(result));
        }
        return responses;
    }

    @GetMapping("/{id}/results")
    public List<ExamResultResponse> results(@PathVariable Long id) {
        List<ExamResultResponse> responses = new ArrayList<>();
        for (ExamResult result : examService.getResults(id)) {
            responses.add(ExamResultResponse.from(result));
        }
        return responses;
    }

    @GetMapping("/students/{studentId}/results")
    public List<ExamResultResponse> studentResults(@PathVariable Long studentId) {
        List<ExamResultResponse> responses = new ArrayList<>();
        for (ExamResult result : examService.getStudentResults(studentId)) {
            responses.add(ExamResultResponse.from(result));
        }
        return responses;
    }

}
