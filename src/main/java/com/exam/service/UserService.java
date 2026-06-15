package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.auth.Role;
import com.exam.dto.RegisterUserRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final AppUserRepository accountRepository;
    private final SchoolClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository repository,
            AppUserRepository accountRepository,
            SchoolClassRepository classRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(User user) {
        if (user.getAccount() != null && user.getAccount().getId() == null) {
            AppUser account = user.getAccount();

            if (accountRepository.existsByUsername(account.getUsername())) {
                throw new BadRequestException("Username is already used");
            }

            if (account.getDisplayName() == null) {
                account.setDisplayName(account.getUsername());
            }

            if (account.getEmail() == null) {
                account.setEmail(account.getUsername() + "@school.local");
            }

            if (account.getPassword() != null && !account.getPassword().startsWith("$2")) {
                account.setPassword(passwordEncoder.encode(account.getPassword()));
            }

            user.setAccount(accountRepository.save(account));
        }

        if (user.getFullName() == null && user.getAccount() != null) {
            user.setFullName(user.getAccount().getDisplayName());
        }

        return repository.save(user);
    }

    @Transactional
    public User registerUser(RegisterUserRequest request) {
        validateRegisterRequest(request);

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already used");
        }

        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already used");
        }

        SchoolClass schoolClass = null;
        if (request.getClassId() != null) {
            schoolClass = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class was not found"));
        }

        AppUser account = new AppUser(
                request.getUsername(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getRole()
        );
        account = accountRepository.save(account);

        User user = new User();
        user.setAccount(account);
        user.setFullName(request.getFullName());
        user.setPassportData(request.getPassportData());
        user.setEntranceExamScore(request.getEntranceExamScore());
        user.setContactInfo(request.getContactInfo());
        user.setBirthDate(request.getBirthDate());
        user.setSchoolClass(schoolClass);

        return repository.save(user);
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User getUserById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found"));
    }

    public List<SchoolClass> getClasses() {
        return classRepository.findByActiveTrueOrderBySPointsDesc();
    }

    private void validateRegisterRequest(RegisterUserRequest request) {
        if (request.getRole() == Role.STUDENT && request.getClassId() == null) {
            throw new BadRequestException("Student account requires a class");
        }

        if (request.getRole() != Role.STUDENT && request.getEntranceExamScore() != null) {
            throw new BadRequestException("Entrance exam score is allowed only for students");
        }
    }
}
