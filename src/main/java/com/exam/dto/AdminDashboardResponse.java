package com.exam.dto;

import java.util.List;

public class AdminDashboardResponse {

    private List<AdminDashboardClassResponse> classes;

    public AdminDashboardResponse(List<AdminDashboardClassResponse> classes) {
        this.classes = classes;
    }

    public List<AdminDashboardClassResponse> getClasses() {
        return classes;
    }
}
