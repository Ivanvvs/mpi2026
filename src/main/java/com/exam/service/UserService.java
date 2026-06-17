package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.auth.Role;
import com.exam.dto.RegisterUserRequest;
import com.exam.dto.UpdateUserRequest;
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
    private final CurrentUserService currentUserService;

    public UserService(
            UserRepository repository,
            AppUserRepository accountRepository,
            SchoolClassRepository classRepository,
            PasswordEncoder passwordEncoder,
            CurrentUserService currentUserService
    ) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.classRepository = classRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
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
        user.setContactInfo(contactInfoOrEmail(request.getContactInfo(), request.getEmail()));
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

    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        validateUpdateRequest(request);

        User user = getUserById(id);
        AppUser account = user.getAccount();
        if (account == null) {
            throw new ResourceNotFoundException("User account was not found");
        }

        accountRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(account.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Email is already used");
                });

        accountRepository.findByUsername(request.getUsername())
                .filter(existing -> !existing.getId().equals(account.getId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("Username is already used");
                });

        SchoolClass schoolClass = null;
        if (request.getClassId() != null) {
            schoolClass = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class was not found"));
        }

        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setDisplayName(request.getFullName());
        account.setRole(request.getRole());
        account.setActive(request.isActive());
        accountRepository.save(account);

        user.setFullName(request.getFullName());
        user.setPassportData(request.getPassportData());
        user.setEntranceExamScore(request.getEntranceExamScore());
        user.setContactInfo(contactInfoOrEmail(request.getContactInfo(), request.getEmail()));
        user.setBirthDate(request.getBirthDate());
        user.setSchoolClass(schoolClass);
        user.setActive(request.isActive());

        return repository.save(user);
    }

    @Transactional
    public User deactivateUser(Long id) {
        User user = getUserById(id);
        user.setActive(false);
        if (user.getAccount() != null) {
            user.getAccount().setActive(false);
            accountRepository.save(user.getAccount());
        }
        return repository.save(user);
    }

    public User getCurrentUser() {
        return currentUserService.getProfile();
    }

    public List<SchoolClass> getClasses() {
        return classRepository.findByActiveTrueOrderBySPointsDesc();
    }

    public SchoolClass getCurrentUserClass() {
        User user = getCurrentUser();
        if (user.getSchoolClass() == null) {
            throw new ResourceNotFoundException("Current user class was not found");
        }
        return user.getSchoolClass();
    }

    private void validateRegisterRequest(RegisterUserRequest request) {
        validateRoleSpecificFields(request.getRole(), request.getClassId(), request.getEntranceExamScore());
    }

    private void validateUpdateRequest(UpdateUserRequest request) {
        validateRoleSpecificFields(request.getRole(), request.getClassId(), request.getEntranceExamScore());
    }

    private void validateRoleSpecificFields(Role role, Long classId, Integer entranceExamScore) {
        if ((role == Role.STUDENT || role == Role.CURATOR) && classId == null) {
            throw new BadRequestException("Student and curator accounts require a class");
        }

        if (role != Role.STUDENT && entranceExamScore != null) {
            throw new BadRequestException("Entrance exam score is allowed only for students");
        }
    }

    private String contactInfoOrEmail(String contactInfo, String email) {
        if (contactInfo != null && !contactInfo.isBlank()) {
            return contactInfo;
        }
        return email;
    }
}
