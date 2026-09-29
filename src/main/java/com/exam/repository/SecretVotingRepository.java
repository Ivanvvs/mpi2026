package com.exam.repository;

import com.exam.model.SecretVoting;
import com.exam.model.VotingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;

@Repository
public interface SecretVotingRepository extends JpaRepository<SecretVoting, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<SecretVoting> findWithLockById(Long id);

    List<SecretVoting> findByStatus(VotingStatus status);
    List<SecretVoting> findBySchoolClassId(Long classId);
    List<SecretVoting> findByCreatedById(Long accountId);
}
