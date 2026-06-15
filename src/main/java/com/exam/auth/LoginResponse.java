package com.exam.auth;

public class LoginResponse {

    private String token;
    private String role;
    private Long accountId;
    private Long userId;
    private String displayName;

    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public LoginResponse(String token, String role, Long accountId, Long userId, String displayName) {
        this.token = token;
        this.role = role;
        this.accountId = accountId;
        this.userId = userId;
        this.displayName = displayName;
    }

    public String getToken() {
        return token;
    }

    public String getRole() {
        return role;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
