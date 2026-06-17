package com.exam.realtime;

import java.time.LocalDateTime;

import static com.exam.util.DateTimeUtils.nowUtc;

public class ExamRealtimeEvent {

    private Long examId;
    private Long userId;
    private String type;
    private String message;
    private LocalDateTime occurredAt = nowUtc();

    public ExamRealtimeEvent() {
        // Required by JSON serialization.
    }

    public ExamRealtimeEvent(Long examId, String type, String message) {
        this.examId = examId;
        this.type = type;
        this.message = message;
    }

    public ExamRealtimeEvent(Long examId, Long userId, String type, String message) {
        this(examId, type, message);
        this.userId = userId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
