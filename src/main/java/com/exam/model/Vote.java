package com.exam.model;

import jakarta.persistence.*;

@Entity
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voting_id", nullable = false)
    private SecretVoting voting;

    /**
     * Deliberately has no relationship to the voter or to VotingReceipt.
     * A vote can therefore be counted without being attributable to a student.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private VotingOption option;

    public Vote() {
        // Required by JPA.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVotingId() {
        return voting == null ? null : voting.getId();
    }

    public SecretVoting getVoting() {
        return voting;
    }

    public void setVoting(SecretVoting voting) {
        this.voting = voting;
    }

    public VotingOption getOption() {
        return option;
    }

    public void setOption(VotingOption option) {
        this.option = option;
    }
}
