package com.exam.dto;

import com.exam.model.SecretVoting;
import com.exam.model.VotingStatus;

import java.time.LocalDateTime;

public class SecretVotingResponse {

    private Long id;
    private String title;
    private String description;
    private SchoolClassResponse schoolClass;
    private VotingStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endsAt;
    private LocalDateTime finishedAt;

    public static SecretVotingResponse from(SecretVoting voting) {
        SecretVotingResponse response = new SecretVotingResponse();
        response.id = voting.getId();
        response.title = voting.getTitle();
        response.description = voting.getDescription();
        response.schoolClass = voting.getSchoolClass() == null ? null : SchoolClassResponse.from(voting.getSchoolClass());
        response.status = voting.getStatus();
        response.startedAt = voting.getStartedAt();
        response.endsAt = voting.getEndsAt();
        response.finishedAt = voting.getFinishedAt();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public SchoolClassResponse getSchoolClass() {
        return schoolClass;
    }

    public VotingStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }
}
