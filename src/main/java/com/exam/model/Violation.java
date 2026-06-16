package com.exam.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "violations")
public class Violation extends SessionUserEntity {

    private String type;

    private String description;

    private String evidencePath;

    private Integer pointsPenalty = 0;

    private boolean affectsExpulsion;

    private LocalDateTime time;

    public Violation() {
        // Required by JPA.
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
