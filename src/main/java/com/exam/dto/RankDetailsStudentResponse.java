package com.exam.dto;

import com.exam.model.User;

public record RankDetailsStudentResponse(Long studentId, String fullName, int sPoints) {
    public static RankDetailsStudentResponse from(User user) {
        return new RankDetailsStudentResponse(user.getId(), user.getFullName(), user.getsPoints());
    }
}
