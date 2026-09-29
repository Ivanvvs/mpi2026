package com.exam.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voting_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"voting_id", "option_id"})
)
public class VotingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voting_id", nullable = false)
    private SecretVoting voting;

    @ManyToOne(optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private VotingOption option;

    @Column(nullable = false)
    private long votesCount;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SecretVoting getVoting() { return voting; }
    public void setVoting(SecretVoting voting) { this.voting = voting; }
    public VotingOption getOption() { return option; }
    public void setOption(VotingOption option) { this.option = option; }
    public long getVotesCount() { return votesCount; }
    public void setVotesCount(long votesCount) { this.votesCount = votesCount; }
    public LocalDateTime getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
}
