package com.exam.dto;

import com.exam.model.Answer;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.Violation;

import java.util.List;

public class ExamDetailsResponse {

    private ExamSession exam;
    private List<QuestionResponse> questions;
    private List<Answer> answers;
    private List<ExamResult> results;
    private List<Violation> violations;
    private ExamAttemptResponse attempt;

    public ExamDetailsResponse(
            ExamSession exam,
            List<QuestionResponse> questions,
            List<Answer> answers,
            List<ExamResult> results,
            List<Violation> violations,
            ExamAttemptResponse attempt
    ) {
        this.exam = exam;
        this.questions = questions;
        this.answers = answers;
        this.results = results;
        this.violations = violations;
        this.attempt = attempt;
    }

    public ExamSession getExam() {
        return exam;
    }

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public List<ExamResult> getResults() {
        return results;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public ExamAttemptResponse getAttempt() {
        return attempt;
    }
}
