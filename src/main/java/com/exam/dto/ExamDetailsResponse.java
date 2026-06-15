package com.exam.dto;

import com.exam.model.Answer;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.Question;

import java.util.List;

public class ExamDetailsResponse {

    private ExamSession exam;
    private List<Question> questions;
    private List<Answer> answers;
    private List<ExamResult> results;

    public ExamDetailsResponse(
            ExamSession exam,
            List<Question> questions,
            List<Answer> answers,
            List<ExamResult> results
    ) {
        this.exam = exam;
        this.questions = questions;
        this.answers = answers;
        this.results = results;
    }

    public ExamSession getExam() {
        return exam;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public List<ExamResult> getResults() {
        return results;
    }
}
