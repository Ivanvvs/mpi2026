package com.exam.repository;

import com.exam.model.VotingOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VotingOptionRepository extends JpaRepository<VotingOption, Long> {
    List<VotingOption> findByVotingId(Long votingId);
}
