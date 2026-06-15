package com.exam.dto;

import com.exam.model.ExamSession;
import com.exam.model.SchoolClass;

import java.util.List;

public class ExamDashboardResponse {

    private List<ExamSession> exams;
    private List<SchoolClassResponse> classes;

    public ExamDashboardResponse(List<ExamSession> exams, List<SchoolClass> classes) {
        this.exams = exams;
        this.classes = classes.stream()
                .map(SchoolClassResponse::from)
                .toList();
    }

    public List<ExamSession> getExams() {
        return exams;
    }

    public List<SchoolClassResponse> getClasses() {
        return classes;
    }
}
