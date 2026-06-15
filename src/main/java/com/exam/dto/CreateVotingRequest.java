package com.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CreateVotingRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long classId;

    private LocalDateTime endsAt;

    @Valid
    private List<VotingOptionRequest> options = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public List<VotingOptionRequest> getOptions() {
        return options;
    }

    public void setOptions(List<VotingOptionRequest> options) {
        this.options = options;
    }
}
