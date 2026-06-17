package com.exam.dto;

import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;

public class AdminDashboardClassResponse {

    private Long id;
    private String name;
    private ClassRank rank;
    private ClassRank proposedRank;
    private int sPoints;
    private long studentCount;
    private boolean rankChangeRequired;

    public static AdminDashboardClassResponse from(
            SchoolClass schoolClass,
            ClassRank proposedRank,
            long studentCount
    ) {
        AdminDashboardClassResponse response = new AdminDashboardClassResponse();
        response.id = schoolClass.getId();
        response.name = schoolClass.getName();
        response.rank = schoolClass.getRank();
        response.proposedRank = proposedRank;
        response.sPoints = schoolClass.getsPoints();
        response.studentCount = studentCount;
        response.rankChangeRequired = proposedRank != null && proposedRank != schoolClass.getRank();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ClassRank getRank() {
        return rank;
    }

    public ClassRank getProposedRank() {
        return proposedRank;
    }

    public int getsPoints() {
        return sPoints;
    }

    public long getStudentCount() {
        return studentCount;
    }

    public boolean isRankChangeRequired() {
        return rankChangeRequired;
    }
}
