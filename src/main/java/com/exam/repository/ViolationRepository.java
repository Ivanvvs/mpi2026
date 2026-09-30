package com.exam.repository;

import com.exam.model.Violation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends JpaRepository<Violation, Long> {

    List<Violation> findBySession_Id(Long sessionId);
    List<Violation> findByUser_Id(Long userId);
    List<Violation> findBySession_IdAndUser_Id(Long sessionId, Long userId);
}
