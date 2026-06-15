package com.exam.dto;

import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;

public class GradeExamRequest {

    @Valid
    private List<StudentScoreRequest> scores = new ArrayList<>();

    public List<StudentScoreRequest> getScores() {
        return scores;
    }

    public void setScores(List<StudentScoreRequest> scores) {
        this.scores = scores;
    }
}
