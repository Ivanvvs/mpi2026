package com.exam.model;

import jakarta.persistence.*;

@Entity
@Table(name = "voting_options")
public class VotingOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voting_id")
    private SecretVoting voting;

    @Column(nullable = false)
    private String label;

    private Long candidateUserId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SecretVoting getVoting() {
        return voting;
    }

    public void setVoting(SecretVoting voting) {
        this.voting = voting;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getCandidateUserId() {
        return candidateUserId;
    }

    public void setCandidateUserId(Long candidateUserId) {
        this.candidateUserId = candidateUserId;
    }
}
