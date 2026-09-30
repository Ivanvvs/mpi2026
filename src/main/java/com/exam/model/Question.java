package com.exam.model;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question extends QuestionFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private ExamSession session;

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
        return session == null ? null : session.getId();
    }

    public ExamSession getSession() {
        return session;
    }

    public void setSession(ExamSession session) {
        this.session = session;
    }

    public void setType(String type) {
        setType(QuestionType.valueOf(type));
    }
}
