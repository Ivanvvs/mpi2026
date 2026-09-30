package com.exam.service;

import com.exam.dto.ViolationRequest;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ExamSession;
import com.exam.model.Violation;
import com.exam.model.User;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.UserRepository;
import com.exam.repository.ViolationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.exam.util.DateTimeUtils.nowUtc;

@Service
public class ViolationService {

    private final ViolationRepository repository;
    private final ExamSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ViolationService(
            ViolationRepository repository,
            ExamSessionRepository sessionRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public Violation reportViolation(Violation violation) {
        if (violation.getTime() == null) {
            violation.setTime(nowUtc());
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
        violation.setUser(user);
        return reportViolation(violation);
    }

    public Violation reportViolation(ViolationRequest request) {
        Violation violation = request.toViolation();
        violation.setSession(getSession(request.sessionId()));
        violation.setUser(getUser(request.userId()));
        return reportViolation(violation);
    }

    public Violation reportCurrentUserViolation(ViolationRequest request) {
        Violation violation = request.toViolation();
        violation.setSession(getSession(request.sessionId()));
        violation.setUser(currentUserService.getProfile());
        return reportViolation(violation);
    }

    public List<Violation> getViolationsBySession(Long sessionId) {
        return repository.findBySession_Id(sessionId);
    }

    private ExamSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam session was not found"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found"));
    }
}
