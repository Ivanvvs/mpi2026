package com.exam.service;

import com.exam.model.Answer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnswerService {

    private final ExamService examService;

    public AnswerService(ExamService examService) {
        this.examService = examService;
    }

    public Answer saveAnswer(Long sessionId, Long userId, Answer answer) {
        return examService.saveAnswer(
                sessionId,
                userId,
                answer.getQuestionId(),
                answer.getText(),
                answer.isFinalSubmitted()
        );
    }

    public Answer updateAnswer(Long sessionId, Long id, Answer updated) {
        return examService.saveAnswer(
                sessionId,
                updated.getUserId(),
                updated.getQuestionId(),
                updated.getText(),
                updated.isFinalSubmitted()
        );
    }

    public List<Answer> getAnswers(Long sessionId) {
        return examService.getAnswers(sessionId);
    }
}
