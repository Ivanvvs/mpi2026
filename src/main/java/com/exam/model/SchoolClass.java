package com.exam.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private ClassRank rank = ClassRank.D;

    private int sPoints;

    private boolean active = true;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public SchoolClass() {
    }

    public SchoolClass(String name, ClassRank rank, int sPoints) {
        this.name = name;
        this.rank = rank;
        this.sPoints = sPoints;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClassRank getRank() {
        return rank;
    }

    public void setRank(ClassRank rank) {
        this.rank = rank;
        this.updatedAt = LocalDateTime.now();
    }

    public int getsPoints() {
        return sPoints;
    }

    public void setsPoints(int sPoints) {
        this.sPoints = sPoints;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
