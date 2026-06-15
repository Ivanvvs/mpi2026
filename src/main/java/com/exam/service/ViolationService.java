package com.exam.service;

import com.exam.model.Violation;
import com.exam.repository.ViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ViolationService {

    @Autowired
    private ViolationRepository repository;

    public Violation reportViolation(Violation violation) {
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "ViolationDetected will be implemented in the next iterations"
        );
    }

    public List<Violation> getViolationsBySession(Long sessionId) {
        return repository.findBySessionId(sessionId);
    }
}
