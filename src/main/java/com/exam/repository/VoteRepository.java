package com.exam.repository;

import com.exam.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    List<Vote> findByVotingId(Long votingId);
    boolean existsByVotingIdAndAnonymousVoterHash(Long votingId, String anonymousVoterHash);
}
