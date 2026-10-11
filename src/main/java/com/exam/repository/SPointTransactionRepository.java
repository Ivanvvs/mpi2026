package com.exam.repository;

import com.exam.model.SPointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SPointTransactionRepository extends JpaRepository<SPointTransaction, Long> {

    List<SPointTransaction> findByUser_IdAndSchoolClass_IdOrderByCreatedAtDescIdDesc(Long userId, Long classId);
}
