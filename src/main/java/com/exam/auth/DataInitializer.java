package com.exam.auth;

import com.exam.model.ClassRank;
import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import com.exam.model.Question;
import com.exam.model.QuestionType;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final UserRepository domainUserRepository;
    private final ExamSessionRepository examSessionRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DataInitializer(
            AppUserRepository userRepository,
            SchoolClassRepository classRepository,
            UserRepository domainUserRepository,
            ExamSessionRepository examSessionRepository,
            QuestionRepository questionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${demo.default-password:}") String demoPassword
    ) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.domainUserRepository = domainUserRepository;
        this.examSessionRepository = examSessionRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword == null || demoPassword.isBlank()
                ? UUID.randomUUID().toString()
                : demoPassword;
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
        AppUser examiner = seedUser("examiner", "examiner@school.local", "Examiner Demo", Role.EXAMINER, null);
        seedUser("admin", "admin@school.local", "Admin Demo", Role.ADMIN, null);

        seedDemoExam(defaultClass, examiner);
    }

    private void seedClass(String name, ClassRank rank, int sPoints) {
        if (classRepository.findByName(name).isEmpty()) {
            classRepository.save(new SchoolClass(name, rank, sPoints));
        }
    }

    private AppUser seedUser(String username, String email, String displayName, Role role, SchoolClass schoolClass) {
        AppUser account = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new AppUser(
                    username,
                    email,
                    passwordEncoder.encode(demoPassword),
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

        return account;
    }

    private void seedDemoExam(SchoolClass schoolClass, AppUser examiner) {
        if (schoolClass == null || examiner == null) {
            return;
        }

        if (examSessionRepository.existsByTitleAndSubjectAndSchoolClassId("test", "test", schoolClass.getId())) {
            return;
        }

        ExamSession exam = new ExamSession();
        exam.setTitle("test");
        exam.setSubject("test");
        exam.setSchoolClass(schoolClass);
        exam.setCreatedBy(examiner);
        exam.setDescription("Demo exam for the main use case");
        exam.setDurationMinutes(30);
        exam.setTotalQuestions(1);
        exam.setStatus(ExamStatus.PREPARED);
        exam = examSessionRepository.save(exam);

        Question question = new Question();
        question.setSessionId(exam.getId());
        question.setOrderIndex(1);
        question.setText("1 + 1");
        question.setType(QuestionType.TEXT);
        question.setCorrectAnswer("2");
        question.setMaxScore(1);
        questionRepository.save(question);
    }
}
