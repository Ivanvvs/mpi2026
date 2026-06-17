package com.exam.dto;

import java.util.List;

public class ExamDetailsResponse {

    private ExamSessionDTO exam;
    private List<QuestionResponse> questions;
    private List<AnswerDTO> answers;
    private List<ExamResultResponse> results;
    private List<ViolationDTO> violations;
    private ExamAttemptResponse attempt;
    private List<ExamStudentAttemptResponse> attempts;

    public ExamDetailsResponse(
            ExamSessionDTO exam,
            List<QuestionResponse> questions,
            List<AnswerDTO> answers,
            List<ExamResultResponse> results,
            List<ViolationDTO> violations,
            ExamAttemptResponse attempt,
            List<ExamStudentAttemptResponse> attempts
    ) {
        this.exam = exam;
        this.questions = questions;
        this.answers = answers;
        this.results = results;
        this.violations = violations;
        this.attempt = attempt;
        this.attempts = attempts;
    }

    public ExamSessionDTO getExam() {
        return exam;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public List<AnswerDTO> getAnswers() {
        return answers;
    }

    public List<ExamResultResponse> getResults() {
        return results;
    }

    public List<ViolationDTO> getViolations() {
        return violations;
    }

    public ExamAttemptResponse getAttempt() {
        return attempt;
    }

    public List<ExamStudentAttemptResponse> getAttempts() {
        return attempts;
    }
}
