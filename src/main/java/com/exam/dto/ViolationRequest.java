package com.exam.dto;

import com.exam.model.Violation;

import java.time.LocalDateTime;

public class ViolationRequest {

    private Long sessionId;
    private Long userId;
    private String type;
    private String description;
    private String evidencePath;
    private Integer pointsPenalty;
    private boolean affectsExpulsion;
    private LocalDateTime time;

    public Violation toViolation() {
        Violation violation = new Violation();
        violation.setSessionId(sessionId);
        violation.setUserId(userId);
        violation.setType(type);
        violation.setDescription(description);
        violation.setEvidencePath(evidencePath);
        violation.setPointsPenalty(pointsPenalty);
        violation.setAffectsExpulsion(affectsExpulsion);
        violation.setTime(time);
        return violation;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidencePath() {
        return evidencePath;
    }

    public void setEvidencePath(String evidencePath) {
        this.evidencePath = evidencePath;
    }

    public Integer getPointsPenalty() {
        return pointsPenalty;
    }

    public void setPointsPenalty(Integer pointsPenalty) {
        this.pointsPenalty = pointsPenalty;
    }

    public boolean isAffectsExpulsion() {
        return affectsExpulsion;
    }

    public void setAffectsExpulsion(boolean affectsExpulsion) {
        this.affectsExpulsion = affectsExpulsion;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
