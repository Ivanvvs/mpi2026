package com.exam.repository;

import com.exam.model.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findBySessionId(Long sessionId);
    List<ExamResult> findByStudentId(Long studentId);
    Optional<ExamResult> findBySessionIdAndStudentId(Long sessionId, Long studentId);
}
