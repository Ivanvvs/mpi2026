package com.exam.realtime;

import java.time.LocalDateTime;

import static com.exam.util.DateTimeUtils.nowUtc;

public class ExamRealtimeEvent {

    private Long examId;
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

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
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
