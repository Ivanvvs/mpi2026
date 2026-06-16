package com.exam.model;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question extends QuestionFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sessionId;

    public Question() {
        // Required by JPA.
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setType(String type) {
        setType(QuestionType.valueOf(type));
    }
}
