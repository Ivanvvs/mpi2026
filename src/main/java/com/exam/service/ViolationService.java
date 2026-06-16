package com.exam.service;

import com.exam.model.Violation;
import com.exam.model.User;
import com.exam.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViolationService {

    private final ViolationRepository repository;
    private final CurrentUserService currentUserService;

    public ViolationService(
            ViolationRepository repository,
            CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
    }

    public Violation reportViolation(Violation violation) {
        if (violation.getTime() == null) {
            violation.setTime(LocalDateTime.now());
        }
        if (violation.getType() == null || violation.getType().isBlank()) {
            violation.setType("EXAM_RULE_VIOLATION");
        }
        if (violation.getPointsPenalty() == null) {
            violation.setPointsPenalty(0);
        }
        return repository.save(violation);
    }

    public Violation reportCurrentUserViolation(Violation violation) {
        User user = currentUserService.getProfile();
        violation.setUserId(user.getId());
        return reportViolation(violation);
    }

    public List<Violation> getViolationsBySession(Long sessionId) {
        return repository.findBySessionId(sessionId);
    }
}
