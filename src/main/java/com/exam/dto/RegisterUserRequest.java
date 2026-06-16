package com.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterUserRequest extends UserProfileRequest {

    @NotBlank
    @Size(min = 4, max = 100)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
