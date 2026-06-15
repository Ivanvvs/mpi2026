package com.exam.repository;

import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    List<ExamSession> findByStatus(ExamStatus status);
    List<ExamSession> findBySchoolClassId(Long classId);
}
