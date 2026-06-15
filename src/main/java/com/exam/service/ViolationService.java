package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.Violation;
import com.exam.model.User;
import com.exam.repository.UserRepository;
import com.exam.repository.ViolationRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ViolationService {

    private final ViolationRepository repository;
    private final AppUserRepository appUserRepository;
    private final UserRepository userRepository;

    public ViolationService(
            ViolationRepository repository,
            AppUserRepository appUserRepository,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.appUserRepository = appUserRepository;
        this.userRepository = userRepository;
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
        User user = currentDomainUser();
        violation.setUserId(user.getId());
        return reportViolation(violation);
    }

    public List<Violation> getViolationsBySession(Long sessionId) {
        return repository.findBySessionId(sessionId);
    }

    private User currentDomainUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AppUser account = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
        return userRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile was not found"));
    }
}
