package com.exam.dto;

public class UserDTO {

    private String username;
    private String role;

    public UserDTO() {
        // Required by JSON deserialization.
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
