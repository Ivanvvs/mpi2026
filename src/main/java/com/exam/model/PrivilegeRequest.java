package com.exam.model;

import com.exam.auth.AppUser;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "privilege_requests")
public class PrivilegeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne
    @JoinColumn(name = "requested_by_id")
    private AppUser requestedBy;

    private String privilegeTitle;

    @Column(length = 2000)
    private String studentWishes;

    @Column(name = "required_s_points")
    private int requiredSPoints;

    @Enumerated(EnumType.STRING)
    private PrivilegeRequestStatus status = PrivilegeRequestStatus.PENDING;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    public AppUser getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(AppUser requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getPrivilegeTitle() {
        return privilegeTitle;
    }

    public void setPrivilegeTitle(String privilegeTitle) {
        this.privilegeTitle = privilegeTitle;
    }

    public String getStudentWishes() {
        return studentWishes;
    }

    public void setStudentWishes(String studentWishes) {
        this.studentWishes = studentWishes;
    }

    public int getRequiredSPoints() {
        return requiredSPoints;
    }

    public void setRequiredSPoints(int requiredSPoints) {
        this.requiredSPoints = requiredSPoints;
    }

    public PrivilegeRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PrivilegeRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
