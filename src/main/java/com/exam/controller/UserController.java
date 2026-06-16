package com.exam.controller;

import com.exam.dto.RegisterUserRequest;
import com.exam.dto.SchoolClassResponse;
import com.exam.dto.UpdateUserRequest;
import com.exam.dto.UserResponse;
import com.exam.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.from(userService.getCurrentUser());
    }

    @GetMapping("/me/class")
    public SchoolClassResponse myClass() {
        return SchoolClassResponse.from(userService.getCurrentUserClass());
    }

    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return UserResponse.from(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public UserResponse deactivate(@PathVariable Long id) {
        return UserResponse.from(userService.deactivateUser(id));
    }

    @GetMapping("/classes")
    public List<SchoolClassResponse> getClasses() {
        return userService.getClasses().stream()
                .map(SchoolClassResponse::from)
                .toList();
    }
}
