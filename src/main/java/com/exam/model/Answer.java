package com.exam.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import static com.exam.util.DateTimeUtils.nowUtc;

@Entity
@Table(name = "answers")
public class Answer extends SessionUserEntity {

    private Long questionId;

    @Column(length = 2000)
    private String text;

    private LocalDateTime savedAt = nowUtc();

    private Integer score;

    private boolean finalSubmitted;

    public Answer() {
        // Required by JPA.
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public boolean isFinalSubmitted() {
        return finalSubmitted;
    }

    public void setFinalSubmitted(boolean finalSubmitted) {
        this.finalSubmitted = finalSubmitted;
    }
}
