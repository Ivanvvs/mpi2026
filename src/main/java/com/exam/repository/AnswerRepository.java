package com.exam.repository;

import com.exam.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySessionId(Long sessionId);
    List<Answer> findBySessionIdAndUserId(Long sessionId, Long userId);
    boolean existsBySessionIdAndUserIdAndQuestionId(Long sessionId, Long userId, Long questionId);
}
