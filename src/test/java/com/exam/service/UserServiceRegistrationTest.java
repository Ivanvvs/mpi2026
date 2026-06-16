package com.exam.service;

import com.exam.auth.AppUserRepository;
import com.exam.auth.Role;
import com.exam.dto.RegisterUserRequest;
import com.exam.dto.UpdateUserRequest;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "debug=false",
                "logging.level.root=warn",
                "logging.level.com.exam=info",
                "logging.level.org.springframework=warn"
        }
)
@ActiveProfiles("test")
class UserServiceRegistrationTest {

    private final UserService userService;
    private final SchoolClassRepository classRepository;
    private final UserRepository userRepository;
    private final AppUserRepository accountRepository;

    @Autowired
    UserServiceRegistrationTest(
            UserService userService,
            SchoolClassRepository classRepository,
            UserRepository userRepository,
            AppUserRepository accountRepository
    ) {
        this.userService = userService;
        this.classRepository = classRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @Test
    void registersUsersForAllRoles() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();

        User student = userService.registerUser(request("new-student", Role.STUDENT, schoolClass.getId()));
        User curator = userService.registerUser(request("new-curator", Role.CURATOR, schoolClass.getId()));
        User examiner = userService.registerUser(request("new-examiner", Role.EXAMINER, null));
        User admin = userService.registerUser(request("new-admin", Role.ADMIN, null));

        assertThat(userRepository.findById(student.getId()).orElseThrow().getSchoolClass().getId()).isEqualTo(schoolClass.getId());
        assertThat(userRepository.findById(curator.getId()).orElseThrow().getSchoolClass().getId()).isEqualTo(schoolClass.getId());
        assertThat(accountRepository.findByUsername("new-student").orElseThrow().getRole()).isEqualTo(Role.STUDENT);
        assertThat(accountRepository.findByUsername("new-curator").orElseThrow().getRole()).isEqualTo(Role.CURATOR);
        assertThat(accountRepository.findByUsername("new-examiner").orElseThrow().getRole()).isEqualTo(Role.EXAMINER);
        assertThat(accountRepository.findByUsername("new-admin").orElseThrow().getRole()).isEqualTo(Role.ADMIN);
        assertThat(student.getContactInfo()).isEqualTo("new-student@school.local");
        assertThat(curator.getContactInfo()).isEqualTo("new-curator@school.local");
        assertThat(examiner.getContactInfo()).isEqualTo("new-examiner@school.local");
        assertThat(admin.getContactInfo()).isEqualTo("new-admin@school.local");
        assertThat(examiner.getSchoolClass()).isNull();
        assertThat(admin.getSchoolClass()).isNull();
    }

    @Test
    void updatesAccountAndProfileFields() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        User user = userService.registerUser(request("update-source", Role.STUDENT, schoolClass.getId()));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Updated User");
        request.setUsername("update-target");
        request.setEmail("update-target@school.local");
        request.setRole(Role.CURATOR);
        request.setClassId(schoolClass.getId());
        request.setPassportData("4000 111111");
        request.setContactInfo("updated contact");
        request.setBirthDate(LocalDate.of(2007, Month.MARCH, 15));
        request.setActive(true);

        User updated = userService.updateUser(user.getId(), request);

        assertThat(updated.getFullName()).isEqualTo("Updated User");
        assertThat(updated.getAccount().getUsername()).isEqualTo("update-target");
        assertThat(updated.getAccount().getEmail()).isEqualTo("update-target@school.local");
        assertThat(updated.getAccount().getRole()).isEqualTo(Role.CURATOR);
        assertThat(updated.getSchoolClass().getId()).isEqualTo(schoolClass.getId());
        assertThat(updated.getPassportData()).isEqualTo("4000 111111");
        assertThat(updated.getContactInfo()).isEqualTo("updated contact");
        assertThat(updated.getBirthDate()).isEqualTo(LocalDate.of(2007, Month.MARCH, 15));
    }

    @Test
    void usesEmailAsProfileContactWhenContactInfoIsEmpty() {
        SchoolClass schoolClass = classRepository.findByName("10A").orElseThrow();
        User user = userService.registerUser(request("email-contact", Role.STUDENT, schoolClass.getId()));

        assertThat(user.getContactInfo()).isEqualTo("email-contact@school.local");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("Email Contact Updated");
        request.setUsername("email-contact-updated");
        request.setEmail("email-contact-updated@school.local");
        request.setRole(Role.STUDENT);
        request.setClassId(schoolClass.getId());
        request.setContactInfo("");
        request.setActive(true);

        User updated = userService.updateUser(user.getId(), request);

        assertThat(updated.getContactInfo()).isEqualTo("email-contact-updated@school.local");
    }

    private RegisterUserRequest request(String username, Role role, Long classId) {
        RegisterUserRequest request = new RegisterUserRequest();
        request.setFullName("Test " + username);
        request.setUsername(username);
        request.setEmail(username + "@school.local");
        request.setPassword("1234");
        request.setRole(role);
        request.setClassId(classId);
        if (role == Role.STUDENT) {
            request.setEntranceExamScore(80);
        }
        return request;
    }
}
