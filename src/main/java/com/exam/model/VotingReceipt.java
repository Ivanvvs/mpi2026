package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import static com.exam.util.DateTimeUtils.nowUtc;

@Entity
@Table(
        name = "voting_receipts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"voting_id", "student_id"})
)
public class VotingReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "voting_id")
    private SecretVoting voting;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id")
    private User student;

    private LocalDateTime votedAt = nowUtc();

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

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(LocalDateTime votedAt) {
        this.votedAt = votedAt;
    }
}
