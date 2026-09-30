package com.exam.repository;

import com.exam.model.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySession_Id(Long sessionId);
    List<Answer> findBySession_IdAndUser_Id(Long sessionId, Long userId);
    boolean existsBySession_IdAndUser_IdAndQuestion_Id(Long sessionId, Long userId, Long questionId);
}
