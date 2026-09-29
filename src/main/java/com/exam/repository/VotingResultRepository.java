package com.exam.repository;

import com.exam.model.VotingResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VotingResultRepository extends JpaRepository<VotingResult, Long> {
    List<VotingResult> findByVotingIdOrderByOptionId(Long votingId);
    Optional<VotingResult> findByVotingIdAndOptionId(Long votingId, Long optionId);
}
