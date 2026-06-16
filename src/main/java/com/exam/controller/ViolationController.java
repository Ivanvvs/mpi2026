package com.exam.controller;

import com.exam.dto.ViolationDTO;
import com.exam.model.Violation;
import com.exam.service.ViolationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/violations")
public class ViolationController {

    private final ViolationService violationService;

    public ViolationController(ViolationService violationService) {
        this.violationService = violationService;
    }

    @PostMapping("/report")
    public ViolationDTO report(@RequestBody Violation violation) {
        return ViolationDTO.from(violationService.reportViolation(violation));
    }

    @PostMapping("/report/me")
    public ViolationDTO reportMe(@RequestBody Violation violation) {
        return ViolationDTO.from(violationService.reportCurrentUserViolation(violation));
    }

    @GetMapping("/session/{sessionId}")
    public List<ViolationDTO> getBySession(@PathVariable Long sessionId) {
        return violationService.getViolationsBySession(sessionId).stream().map(ViolationDTO::from).toList();
    }
}
