package com.exam.dto;

import com.exam.model.SchoolClass;

import java.util.List;

public class ExamDashboardResponse {

    private List<ExamSessionDTO> exams;
    private List<SchoolClassResponse> classes;

    public ExamDashboardResponse(List<ExamSessionDTO> exams, List<SchoolClass> classes) {
        this.exams = exams;
        this.classes = classes.stream()
                .map(SchoolClassResponse::from)
                .toList();
    }

    public List<ExamSessionDTO> getExams() {
        return exams;
    }

    public List<SchoolClassResponse> getClasses() {
        return classes;
    }
}
