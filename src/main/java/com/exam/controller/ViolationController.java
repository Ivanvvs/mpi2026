package com.exam.controller;

import com.exam.dto.ViolationDTO;
import com.exam.dto.ViolationRequest;
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
    public ViolationDTO report(@RequestBody ViolationRequest request) {
        return ViolationDTO.from(violationService.reportViolation(request.toViolation()));
    }

    @PostMapping("/report/me")
    public ViolationDTO reportMe(@RequestBody ViolationRequest request) {
        return ViolationDTO.from(violationService.reportCurrentUserViolation(request.toViolation()));
    }

    @GetMapping("/session/{sessionId}")
    public List<ViolationDTO> getBySession(@PathVariable Long sessionId) {
        return violationService.getViolationsBySession(sessionId).stream().map(ViolationDTO::from).toList();
    }
}
