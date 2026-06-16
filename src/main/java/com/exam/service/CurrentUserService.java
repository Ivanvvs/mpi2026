package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.User;
import com.exam.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final AppUserRepository accountRepository;
    private final UserRepository userRepository;

    public CurrentUserService(AppUserRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    public AppUser getAccount() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    public User getProfile() {
        AppUser account = getAccount();
        return userRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User profile was not found"));
    }
}
