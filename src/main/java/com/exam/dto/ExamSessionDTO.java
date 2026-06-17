package com.exam.dto;

import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;

import java.time.LocalDateTime;

public class ExamSessionDTO {

    private Long id;
    private String title;
    private String subject;
    private ExamStatus status;
    private SchoolClassResponse schoolClass;
    private Integer durationMinutes;
    private Integer totalQuestions;
    private String description;
    private boolean active;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public static ExamSessionDTO from(ExamSession session) {
        ExamSessionDTO response = new ExamSessionDTO();
        response.id = session.getId();
        response.title = session.getTitle();
        response.subject = session.getSubject();
        response.status = session.getStatus();
        response.schoolClass = session.getSchoolClass() == null ? null : SchoolClassResponse.from(session.getSchoolClass());
        response.durationMinutes = session.getDurationMinutes();
        response.totalQuestions = session.getTotalQuestions();
        response.description = session.getDescription();
        response.active = session.isActive();
        response.scheduledStartTime = session.getScheduledStartTime();
        response.startTime = session.getStartTime();
        response.endTime = session.getEndTime();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public SchoolClassResponse getSchoolClass() {
        return schoolClass;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getScheduledStartTime() {
        return scheduledStartTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
