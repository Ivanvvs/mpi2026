package com.exam.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import static com.exam.util.DateTimeUtils.nowUtc;

@Entity
@Table(name = "s_point_transactions")
public class SPointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private ExamSession session;

    @Column(name = "points_delta")
    private int pointsDelta;

    @Column(name = "resulting_balance")
    private int resultingBalance;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt = nowUtc();

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    public ExamSession getSession() {
        return session;
    }

    public void setSession(ExamSession session) {
        this.session = session;
    }

    public int getPointsDelta() {
        return pointsDelta;
    }

    public void setPointsDelta(int pointsDelta) {
        this.pointsDelta = pointsDelta;
    }

    public int getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(int resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
