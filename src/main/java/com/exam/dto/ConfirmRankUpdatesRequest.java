package com.exam.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ConfirmRankUpdatesRequest {

    @NotEmpty
    private List<Long> classIds;

    public List<Long> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<Long> classIds) {
        this.classIds = classIds;
    }
}
