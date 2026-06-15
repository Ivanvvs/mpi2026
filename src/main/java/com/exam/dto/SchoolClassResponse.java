package com.exam.dto;

import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;

public class SchoolClassResponse {

    private Long id;
    private String name;
    private ClassRank rank;
    private int sPoints;

    public static SchoolClassResponse from(SchoolClass schoolClass) {
        SchoolClassResponse response = new SchoolClassResponse();
        response.id = schoolClass.getId();
        response.name = schoolClass.getName();
        response.rank = schoolClass.getRank();
        response.sPoints = schoolClass.getsPoints();
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

    public int getsPoints() {
        return sPoints;
    }
}
