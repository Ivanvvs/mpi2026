package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.User;
import com.exam.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
        Optional<AppUser> optionalAccount = accountRepository.findByUsername(username);
        if (!optionalAccount.isPresent()) {
            throw new ResourceNotFoundException("Authenticated user was not found");
        }
        return optionalAccount.get();
    }

    public User getProfile() {
        AppUser account = getAccount();
        Optional<User> optionalProfile = userRepository.findByAccountId(account.getId());
        if (!optionalProfile.isPresent()) {
            throw new ResourceNotFoundException("User profile was not found");
        }
        return optionalProfile.get();
    }
}
