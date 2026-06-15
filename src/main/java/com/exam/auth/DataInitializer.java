package com.exam.auth;

import com.exam.model.ClassRank;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository domainUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            AppUserRepository userRepository,
            SchoolClassRepository classRepository,
            UserRepository domainUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.domainUserRepository = domainUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedClass("10A", ClassRank.A, 2120);
        seedClass("10B", ClassRank.B, 4310);
        seedClass("11A", ClassRank.A, 5240);
        seedClass("11B", ClassRank.C, 2580);

        SchoolClass defaultClass = classRepository.findByName("10A").orElse(null);

        seedUser("student", "student@school.local", "Student Demo", Role.STUDENT, defaultClass);
        seedUser("curator", "curator@school.local", "Curator Demo", Role.CURATOR, defaultClass);
        seedUser("examiner", "examiner@school.local", "Examiner Demo", Role.EXAMINER, null);
        seedUser("admin", "admin@school.local", "Admin Demo", Role.ADMIN, null);
    }

    private void seedClass(String name, ClassRank rank, int sPoints) {
        if (classRepository.findByName(name).isEmpty()) {
            classRepository.save(new SchoolClass(name, rank, sPoints));
        }
    }

    private void seedUser(String username, String email, String displayName, Role role, SchoolClass schoolClass) {
        AppUser account = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new AppUser(
                    username,
                    email,
                    passwordEncoder.encode("1234"),
                    displayName,
                    role
            )));

        if (domainUserRepository.findByAccountId(account.getId()).isEmpty()) {
            User user = new User();
            user.setAccount(account);
            user.setFullName(displayName);
            user.setContactInfo(email);
            user.setSchoolClass(role == Role.STUDENT || role == Role.CURATOR ? schoolClass : null);
            domainUserRepository.save(user);
        }
    }
}
