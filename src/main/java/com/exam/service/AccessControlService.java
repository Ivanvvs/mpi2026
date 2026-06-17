package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.Role;
import com.exam.exception.ForbiddenException;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import org.springframework.stereotype.Service;

@Service
public class AccessControlService {

    private final CurrentUserService currentUserService;

    public AccessControlService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public User currentProfile() {
        return currentUserService.getProfile();
    }

    public Role currentRole() {
        AppUser account = currentProfile().getAccount();
        return account == null ? null : account.getRole();
    }

    public Long currentAccountId() {
        AppUser account = currentProfile().getAccount();
        return account == null ? null : account.getId();
    }

    public Long currentClassId() {
        SchoolClass schoolClass = currentProfile().getSchoolClass();
        return schoolClass == null ? null : schoolClass.getId();
    }

    public boolean hasRole(Role role) {
        return currentRole() == role;
    }

    public boolean owns(AppUser owner) {
        Long accountId = currentAccountId();
        return owner != null && accountId != null && owner.getId().equals(accountId);
    }

    public boolean belongsToClass(SchoolClass schoolClass) {
        Long classId = currentClassId();
        return schoolClass != null && classId != null && schoolClass.getId().equals(classId);
    }

    public boolean belongsToClass(Long classId) {
        Long currentClassId = currentClassId();
        return classId != null && classId.equals(currentClassId);
    }

    public boolean isCurrentProfile(Long userId) {
        return userId != null && userId.equals(currentProfile().getId());
    }

    public void require(boolean allowed, String message) {
        if (!allowed) {
            throw new ForbiddenException(message);
        }
    }
}
