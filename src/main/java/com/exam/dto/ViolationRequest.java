package com.exam.dto;

import com.exam.model.Violation;

import java.time.LocalDateTime;

public record ViolationRequest(
        Long sessionId,
        Long userId,
        String type,
        String description,
        String evidencePath,
        Integer pointsPenalty,
        boolean affectsExpulsion,
        LocalDateTime time
) {

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
}
