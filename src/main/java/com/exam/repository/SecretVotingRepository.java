package com.exam.repository;

import com.exam.model.SecretVoting;
import com.exam.model.VotingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecretVotingRepository extends JpaRepository<SecretVoting, Long> {
    List<SecretVoting> findByStatus(VotingStatus status);
    List<SecretVoting> findBySchoolClassId(Long classId);
    List<SecretVoting> findByCreatedById(Long accountId);
}
