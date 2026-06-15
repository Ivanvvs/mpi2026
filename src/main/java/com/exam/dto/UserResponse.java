package com.exam.dto;

import com.exam.auth.Role;
import com.exam.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private Long accountId;
    private String fullName;
    private String username;
    private String email;
    private Role role;
    private Long classId;
    private String className;
    private String passportData;
    private Integer entranceExamScore;
    private String contactInfo;
    private LocalDate birthDate;
    private boolean active;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.fullName = user.getFullName();
        response.active = user.isActive();
        response.passportData = user.getPassportData();
        response.entranceExamScore = user.getEntranceExamScore();
        response.contactInfo = user.getContactInfo();
        response.birthDate = user.getBirthDate();

        if (user.getAccount() != null) {
            response.accountId = user.getAccount().getId();
            response.username = user.getAccount().getUsername();
            response.email = user.getAccount().getEmail();
            response.role = user.getAccount().getRole();
            response.createdAt = user.getAccount().getCreatedAt();
        }

        if (user.getSchoolClass() != null) {
            response.classId = user.getSchoolClass().getId();
            response.className = user.getSchoolClass().getName();
        }

        return response;
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public String getClassName() {
        return className;
    }

    public Long getClassId() {
        return classId;
    }

    public String getPassportData() {
        return passportData;
    }

    public Integer getEntranceExamScore() {
        return entranceExamScore;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
