package com.exam.auth;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class TestRoleController {

    @GetMapping("/student/test")
    public String studentTest() {
        return "Student endpoint works";
    }

    @GetMapping("/examiner/test")
    public String examinerTest() {
        return "Examiner endpoint works";
    }

    @GetMapping("/curator/test")
    public String curatorTest() {
        return "Curator endpoint works";
    }

    @GetMapping("/admin/test")
    public String adminTest() {
        return "Admin endpoint works";
    }
}
