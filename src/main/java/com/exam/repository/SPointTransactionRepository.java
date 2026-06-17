package com.exam.repository;

import com.exam.model.SPointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SPointTransactionRepository extends JpaRepository<SPointTransaction, Long> {
}
