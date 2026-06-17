package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.Role;
import com.exam.dto.AnswerDTO;
import com.exam.dto.ExamAttemptResponse;
import com.exam.dto.ExamDetailsResponse;
import com.exam.dto.ExamResultResponse;
import com.exam.dto.ExamSessionDTO;
import com.exam.dto.ExamStudentAttemptResponse;
import com.exam.dto.ViolationDTO;
import com.exam.dto.QuestionResponse;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.Answer;
import com.exam.model.ExamAttempt;
import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import com.exam.model.Question;
import com.exam.model.User;
import com.exam.repository.AnswerRepository;
import com.exam.repository.ExamAttemptRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.UserRepository;
import com.exam.repository.ViolationRepository;
import com.exam.realtime.ExamRealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.exam.util.DateTimeUtils.nowUtc;

@Service
public class ExamParticipationService {

    private final ExamLifecycleService examLifecycleService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final ExamAttemptRepository attemptRepository;
    private final UserRepository userRepository;
    private final ViolationRepository violationRepository;
    private final ExamRealtimePublisher realtimePublisher;
    private final CurrentUserService currentUserService;
    private final ExamResultService examResultService;

    public ExamParticipationService(
            ExamLifecycleService examLifecycleService,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            ExamAttemptRepository attemptRepository,
            UserRepository userRepository,
            ViolationRepository violationRepository,
            ExamRealtimePublisher realtimePublisher,
            CurrentUserService currentUserService,
            ExamResultService examResultService
    ) {
        this.examLifecycleService = examLifecycleService;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.attemptRepository = attemptRepository;
        this.userRepository = userRepository;
        this.violationRepository = violationRepository;
        this.realtimePublisher = realtimePublisher;
        this.currentUserService = currentUserService;
        this.examResultService = examResultService;
    }

    public List<Question> getQuestions(Long sessionId) {
        examLifecycleService.getSession(sessionId);
        return questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
    }

    public List<Answer> getAnswers(Long sessionId) {
        examLifecycleService.getSession(sessionId);
        return answerRepository.findBySessionId(sessionId);
    }

    public List<Answer> getStudentAnswers(Long sessionId, Long studentId) {
        examLifecycleService.getSession(sessionId);
        return answerRepository.findBySessionIdAndUserId(sessionId, studentId);
    }

    @Transactional
    public Answer saveAnswer(Long sessionId, Long studentId, Long questionId, String text, boolean finalSubmitted) {
        ExamSession session = examLifecycleService.getSession(sessionId);

        if (session.getStatus() != ExamStatus.ACTIVE) {
            throw new BadRequestException("Answers can be saved only for active exam");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student was not found"));
        validateStudentCanTakeExam(session, student);
        ExamAttempt attempt = getOrCreateAttempt(session, student);
        if (attempt.isSubmitted()) {
            throw new BadRequestException("Submitted exam attempt cannot be changed");
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question was not found"));
        if (!question.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Question does not belong to this exam");
        }

        Answer answer = answerRepository.findBySessionIdAndUserId(sessionId, studentId).stream()
                .filter(item -> item.getQuestionId().equals(questionId))
                .findFirst()
                .orElseGet(Answer::new);

        answer.setSessionId(sessionId);
        answer.setUserId(studentId);
        answer.setQuestionId(questionId);
        answer.setText(text);
        answer.setSavedAt(nowUtc());
        answer.setFinalSubmitted(finalSubmitted);

        Answer saved = answerRepository.save(answer);
        realtimePublisher.publish(sessionId, studentId, "ANSWER_SAVED", "Student answer has been saved");
        return saved;
    }

    @Transactional
    public Answer saveCurrentUserAnswer(Long sessionId, Long questionId, String text, boolean finalSubmitted) {
        User user = currentUserService.getProfile();
        return saveAnswer(sessionId, user.getId(), questionId, text, finalSubmitted);
    }

    @Transactional
    public ExamAttempt submitCurrentUserAttempt(Long sessionId) {
        ExamSession session = examLifecycleService.getSession(sessionId);
        if (session.getStatus() != ExamStatus.ACTIVE) {
            throw new BadRequestException("Only active exam attempt can be submitted");
        }

        User user = currentUserService.getProfile();
        validateStudentCanTakeExam(session, user);
        ExamAttempt attempt = getOrCreateAttempt(session, user);
        if (attempt.getSubmittedAt() == null) {
            attempt.setSubmittedAt(nowUtc());
            attempt = attemptRepository.save(attempt);
            answerRepository.findBySessionIdAndUserId(sessionId, user.getId()).forEach(answer -> {
                answer.setFinalSubmitted(true);
                answerRepository.save(answer);
            });
            realtimePublisher.publish(sessionId, user.getId(), "ATTEMPT_SUBMITTED", "Student attempt has been submitted");
        }
        return attempt;
    }

    public ExamDetailsResponse getDetails(Long sessionId) {
        ExamSession session = examLifecycleService.getSession(sessionId);
        AppUser account = currentUserService.getAccount();
        boolean student = account.getRole() == Role.STUDENT;
        boolean includeFullExamData = account.getRole() == Role.EXAMINER || account.getRole() == Role.ADMIN;

        User domainUser = null;
        ExamAttemptResponse attempt = ExamAttemptResponse.empty();
        List<Answer> answers = List.of();
        var results = List.copyOf(examResultService.getResultsForDetails(sessionId, null));
        List<com.exam.model.Violation> violations = List.of();
        List<ExamStudentAttemptResponse> attempts = List.of();

        if (student) {
            domainUser = currentUserService.getProfile();
            validateStudentCanTakeExam(session, domainUser);
            ExamAttempt examAttempt = getOrCreateAttempt(session, domainUser);
            attempt = ExamAttemptResponse.from(examAttempt);
            answers = answerRepository.findBySessionIdAndUserId(sessionId, domainUser.getId());
            results = session.getStatus() == ExamStatus.FINISHED
                    ? examResultService.getResultsForDetails(sessionId, domainUser.getId())
                    : List.of();
        } else if (includeFullExamData) {
            answers = getAnswers(sessionId);
            results = examResultService.getResultsForDetails(sessionId, null);
            violations = violationRepository.findBySessionId(sessionId);
            attempts = attemptRepository.findBySessionId(sessionId).stream()
                    .map(ExamStudentAttemptResponse::from)
                    .toList();
        } else {
            results = List.of();
        }

        return new ExamDetailsResponse(
                ExamSessionDTO.from(session),
                getQuestions(sessionId).stream()
                        .map(question -> QuestionResponse.from(question, includeFullExamData))
                        .toList(),
                answers.stream().map(AnswerDTO::from).toList(),
                results.stream().map(ExamResultResponse::from).toList(),
                violations.stream().map(ViolationDTO::from).toList(),
                attempt,
                attempts
        );
    }

    private ExamAttempt getOrCreateAttempt(ExamSession session, User student) {
        return attemptRepository.findBySessionIdAndStudentId(session.getId(), student.getId())
                .orElseGet(() -> {
                    ExamAttempt attempt = new ExamAttempt();
                    attempt.setSession(session);
                    attempt.setStudent(student);
                    attempt.setStartedAt(nowUtc());
                    return attemptRepository.save(attempt);
                });
    }

    private void validateStudentCanTakeExam(ExamSession session, User student) {
        if (!student.isActive()) {
            throw new BadRequestException("Inactive student cannot take exam");
        }

        if (student.getAccount() == null || student.getAccount().getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can submit exam answers");
        }

        if (session.getSchoolClass() != null
                && (student.getSchoolClass() == null
                || !session.getSchoolClass().getId().equals(student.getSchoolClass().getId()))) {
            throw new BadRequestException("Student does not belong to exam class");
        }
    }
}
