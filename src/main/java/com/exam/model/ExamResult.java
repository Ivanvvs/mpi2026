package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import static com.exam.util.DateTimeUtils.nowUtc;

@Entity
@Table(
        name = "exam_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_id"})
)
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private ExamSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User student;

    private int rawScore;

    private int violationPenalty;

    private int finalScore;

    private Integer rankPlace;

    private LocalDateTime gradedAt = nowUtc();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExamSession getSession() {
        return session;
    }

    public void setSession(ExamSession session) {
        this.session = session;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public int getRawScore() {
        return rawScore;
    }

    public void setRawScore(int rawScore) {
        this.rawScore = rawScore;
    }

    public int getViolationPenalty() {
        return violationPenalty;
    }

    public void setViolationPenalty(int violationPenalty) {
        this.violationPenalty = violationPenalty;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(int finalScore) {
        this.finalScore = finalScore;
    }

    public Integer getRankPlace() {
        return rankPlace;
    }

    public void setRankPlace(Integer rankPlace) {
        this.rankPlace = rankPlace;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }
}
