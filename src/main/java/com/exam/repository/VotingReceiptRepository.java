package com.exam.repository;

import com.exam.model.VotingReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingReceiptRepository extends JpaRepository<VotingReceipt, Long> {
    boolean existsByVotingIdAndStudentId(Long votingId, Long studentId);
}
