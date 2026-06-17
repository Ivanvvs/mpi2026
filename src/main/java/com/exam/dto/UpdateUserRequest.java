package com.exam.dto;

public class UpdateUserRequest extends UserProfileRequest {

    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
