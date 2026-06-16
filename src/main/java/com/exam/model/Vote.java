package com.exam.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"voting_id", "anonymous_voter_hash"})
)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voting_id", nullable = false)
    private Long votingId;

    @Column(name = "encrypted_value", nullable = false)
    private String encryptedValue;

    @Column(name = "anonymous_voter_hash", nullable = false)
    private String anonymousVoterHash;

    public Vote() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVotingId() {
        return votingId;
    }

    public void setVotingId(Long votingId) {
        this.votingId = votingId;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public String getAnonymousVoterHash() {
        return anonymousVoterHash;
    }

    public void setAnonymousVoterHash(String anonymousVoterHash) {
        this.anonymousVoterHash = anonymousVoterHash;
    }
}
