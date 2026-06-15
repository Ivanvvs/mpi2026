package com.exam.auth;

import com.exam.exception.BadRequestException;
import com.exam.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AppUserRepository userRepository;
    private final UserRepository domainUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            AppUserRepository userRepository,
            UserRepository domainUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.domainUserRepository = domainUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new BadRequestException("User account is inactive");
        }

        String token = jwtService.generateToken(user);
        Long domainUserId = domainUserRepository.findByAccountId(user.getId())
                .map(com.exam.model.User::getId)
                .orElse(null);

        return new LoginResponse(token, user.getRole().name(), user.getId(), domainUserId, user.getDisplayName());
    }
}
