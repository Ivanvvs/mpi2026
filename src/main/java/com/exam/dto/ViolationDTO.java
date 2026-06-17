package com.exam.dto;

import com.exam.model.Violation;

import java.time.LocalDateTime;

public class ViolationDTO {

    private Long id;
    private Long sessionId;
    private Long userId;
    private String type;
    private String description;
    private String evidencePath;
    private Integer pointsPenalty;
    private boolean affectsExpulsion;
    private LocalDateTime time;

    public static ViolationDTO from(Violation violation) {
        ViolationDTO response = new ViolationDTO();
        response.id = violation.getId();
        response.sessionId = violation.getSessionId();
        response.userId = violation.getUserId();
        response.type = violation.getType();
        response.description = violation.getDescription();
        response.evidencePath = violation.getEvidencePath();
        response.pointsPenalty = violation.getPointsPenalty();
        response.affectsExpulsion = violation.isAffectsExpulsion();
        response.time = violation.getTime();
        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getEvidencePath() {
        return evidencePath;
    }

    public Integer getPointsPenalty() {
        return pointsPenalty;
    }

    public boolean isAffectsExpulsion() {
        return affectsExpulsion;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
