package com.exam.auth;

public class LoginRequest {

    private String username;
    private String password;

    public LoginRequest() {
        // Required by JSON deserialization.
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
