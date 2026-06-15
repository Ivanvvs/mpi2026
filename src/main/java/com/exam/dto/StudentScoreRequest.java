package com.exam.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class StudentScoreRequest {

    @NotNull
    private Long studentId;

    @Min(0)
    private int rawScore;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public int getRawScore() {
        return rawScore;
    }

    public void setRawScore(int rawScore) {
        this.rawScore = rawScore;
    }
}
