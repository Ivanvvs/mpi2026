package com.exam.controller;

import com.exam.model.Violation;
import com.exam.service.ViolationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/violations")
@CrossOrigin(origins = "*")
public class ViolationController {

    @Autowired
    private ViolationService violationService;

    @PostMapping("/report")
    public Violation report(@RequestBody Violation violation) {
        return violationService.reportViolation(violation);
    }

    @PostMapping("/report/me")
    public Violation reportMe(@RequestBody Violation violation) {
        return violationService.reportCurrentUserViolation(violation);
    }

    @GetMapping("/session/{sessionId}")
    public List<Violation> getBySession(@PathVariable Long sessionId) {
        return violationService.getViolationsBySession(sessionId);
    }
}
