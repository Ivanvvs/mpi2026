package com.exam.model;

import com.exam.auth.AppUser;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "account_id")
    private AppUser account;

    private String fullName;

    @Column(length = 128)
    private String passportData;

    private Integer entranceExamScore;

    private String contactInfo;

    private LocalDate birthDate;

    @ManyToOne
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    private boolean active = true;

    public User() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getAccount() {
        return account;
    }

    public void setAccount(AppUser account) {
        this.account = account;
    }

    public String getUsername() {
        return account == null ? null : account.getUsername();
    }

    public void setUsername(String username) {
        if (this.account == null) {
            this.account = new AppUser();
        }
        this.account.setUsername(username);
    }

    public String getPassword() {
        return account == null ? null : account.getPassword();
    }

    public void setPassword(String password) {
        if (this.account == null) {
            this.account = new AppUser();
        }
        this.account.setPassword(password);
    }

    public String getRole() {
        return account == null || account.getRole() == null ? null : account.getRole().name();
    }

    public void setRole(String role) {
        if (this.account == null) {
            this.account = new AppUser();
        }
        this.account.setRole(com.exam.auth.Role.valueOf(role));
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPassportData() {
        return passportData;
    }

    public void setPassportData(String passportData) {
        this.passportData = passportData;
    }

    public Integer getEntranceExamScore() {
        return entranceExamScore;
    }

    public void setEntranceExamScore(Integer entranceExamScore) {
        this.entranceExamScore = entranceExamScore;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
