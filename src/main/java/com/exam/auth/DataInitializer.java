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
        SchoolClass class10A = seedClass("10A", ClassRank.A);
        SchoolClass class10B = seedClass("10B", ClassRank.B);
        SchoolClass class11A = seedClass("11A", ClassRank.A);
        SchoolClass class11B = seedClass("11B", ClassRank.C);

        seedUser("student", "student@school.local", "Student Demo", Role.STUDENT, class10A, 850);
        seedUser("curator", "curator@school.local", "Curator Demo", Role.CURATOR, class10A, 0);
        AppUser examiner = seedUser("examiner", "examiner@school.local", "Examiner Demo", Role.EXAMINER, null, 0);
        seedUser("admin", "admin@school.local", "Admin Demo", Role.ADMIN, null, 0);

        seedUser("hirata", "hirata@school.local", "Хирата Ёскэ", Role.STUDENT, class10B, 920);
        seedUser("karuizawa", "karuizawa@school.local", "Каруидзава Кэй", Role.STUDENT, class10B, 760);
        seedUser("sakayanagi", "sakayanagi@school.local", "Сакаянаги Арису", Role.STUDENT, class11A, 990);
        seedUser("hashimoto", "hashimoto@school.local", "Хасимото Масаюки", Role.STUDENT, class11A, 870);
        seedUser("ryuen", "ryuen@school.local", "Рюэн Какэру", Role.STUDENT, class11B, 800);
        seedUser("ishizaki", "ishizaki@school.local", "Исидзаки Дайти", Role.STUDENT, class11B, 640);

        recalculateClassBalance(class10A);
        recalculateClassBalance(class10B);
        recalculateClassBalance(class11A);
        recalculateClassBalance(class11B);

        seedDemoExam(class10A, examiner);
    }

    private SchoolClass seedClass(String name, ClassRank rank) {
        return classRepository.findByName(name)
                .orElseGet(() -> classRepository.save(new SchoolClass(name, rank, 0)));
    }

    private AppUser seedUser(String username, String email, String displayName, Role role, SchoolClass schoolClass, int sPoints) {
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
            user.setsPoints(sPoints);
            domainUserRepository.save(user);
        }

        return account;
    }

    private void recalculateClassBalance(SchoolClass schoolClass) {
        schoolClass.setsPoints(domainUserRepository.sumSPointsBySchoolClassId(schoolClass.getId()));
        classRepository.save(schoolClass);
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
