package com.exam.repository;

import com.exam.model.ExamAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<ExamAttempt> findBySessionId(Long sessionId);
}
