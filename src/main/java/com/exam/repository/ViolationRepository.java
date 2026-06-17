package com.exam.repository;

import com.exam.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    List<Violation> findBySessionId(Long sessionId);
    List<Violation> findByUserId(Long userId);
    List<Violation> findBySessionIdAndUserId(Long sessionId, Long userId);
}
