package com.exam.controller;

import com.exam.model.User;
import com.exam.dto.RegisterUserRequest;
import com.exam.dto.SchoolClassResponse;
import com.exam.dto.UserResponse;
import com.exam.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/create")
    public User create(@RequestBody User user) {
        return userService.createUser(user);
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(userService.registerUser(request));
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAllUsers().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/classes")
    public List<SchoolClassResponse> getClasses() {
        return userService.getClasses().stream()
                .map(SchoolClassResponse::from)
                .toList();
    }
}
