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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
        return questionRepository.findBySession_IdOrderByOrderIndexAsc(sessionId);
    }

    public List<Answer> getAnswers(Long sessionId) {
        examLifecycleService.getSession(sessionId);
        return answerRepository.findBySession_Id(sessionId);
    }

    public List<Answer> getStudentAnswers(Long sessionId, Long studentId) {
        examLifecycleService.getSession(sessionId);
        return answerRepository.findBySession_IdAndUser_Id(sessionId, studentId);
    }

    @Transactional
    public Answer saveAnswer(Long sessionId, Long studentId, Long questionId, String text, boolean finalSubmitted) {
        ExamSession session = examLifecycleService.getSession(sessionId);

        if (session.getStatus() != ExamStatus.ACTIVE) {
            throw new BadRequestException("Answers can be saved only for active exam");
        }

        Optional<User> optionalStudent = userRepository.findById(studentId);
        if (!optionalStudent.isPresent()) {
            throw new ResourceNotFoundException("Student was not found");
        }
        User student = optionalStudent.get();
        validateStudentCanTakeExam(session, student);
        ExamAttempt attempt = getOrCreateAttempt(session, student);
        if (attempt.isSubmitted()) {
            throw new BadRequestException("Submitted exam attempt cannot be changed");
        }

        Optional<Question> optionalQuestion = questionRepository.findById(questionId);
        if (!optionalQuestion.isPresent()) {
            throw new ResourceNotFoundException("Question was not found");
        }
        Question question = optionalQuestion.get();
        if (!question.getSessionId().equals(sessionId)) {
            throw new BadRequestException("Question does not belong to this exam");
        }

        Answer answer = null;
        List<Answer> existingAnswers = answerRepository.findBySession_IdAndUser_Id(sessionId, studentId);
        for (Answer existingAnswer : existingAnswers) {
            if (existingAnswer.getQuestionId().equals(questionId)) {
                answer = existingAnswer;
                break;
            }
        }
        if (answer == null) {
            answer = new Answer();
        }

        answer.setSession(session);
        answer.setUser(student);
        answer.setQuestion(question);
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
            List<Answer> answers = answerRepository.findBySession_IdAndUser_Id(sessionId, user.getId());
            for (Answer answer : answers) {
                answer.setFinalSubmitted(true);
                answerRepository.save(answer);
            }
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
        List<Answer> answers = Collections.emptyList();
        List<com.exam.model.ExamResult> results = new ArrayList<>(examResultService.getResultsForDetails(sessionId, null));
        List<com.exam.model.Violation> violations = Collections.emptyList();
        List<ExamStudentAttemptResponse> attempts = Collections.emptyList();

        if (student) {
            domainUser = currentUserService.getProfile();
            validateStudentCanTakeExam(session, domainUser);
            ExamAttempt examAttempt = getOrCreateAttempt(session, domainUser);
            attempt = ExamAttemptResponse.from(examAttempt);
            answers = answerRepository.findBySession_IdAndUser_Id(sessionId, domainUser.getId());
            results = session.getStatus() == ExamStatus.FINISHED
                    ? examResultService.getResultsForDetails(sessionId, domainUser.getId())
                    : Collections.emptyList();
        } else if (includeFullExamData) {
            answers = getAnswers(sessionId);
            results = examResultService.getResultsForDetails(sessionId, null);
            violations = violationRepository.findBySession_Id(sessionId);
            List<ExamAttempt> examAttempts = attemptRepository.findBySessionId(sessionId);
            attempts = new ArrayList<>();
            for (ExamAttempt examAttempt : examAttempts) {
                attempts.add(ExamStudentAttemptResponse.from(examAttempt));
            }
        } else {
            results = Collections.emptyList();
        }

        List<QuestionResponse> questionResponses = new ArrayList<>();
        for (Question question : getQuestions(sessionId)) {
            questionResponses.add(QuestionResponse.from(question, includeFullExamData));
        }
        List<AnswerDTO> answerResponses = new ArrayList<>();
        for (Answer answer : answers) {
            answerResponses.add(AnswerDTO.from(answer));
        }
        List<ExamResultResponse> resultResponses = new ArrayList<>();
        for (com.exam.model.ExamResult result : results) {
            resultResponses.add(ExamResultResponse.from(result));
        }
        List<ViolationDTO> violationResponses = new ArrayList<>();
        for (com.exam.model.Violation violation : violations) {
            violationResponses.add(ViolationDTO.from(violation));
        }

        return new ExamDetailsResponse(
                ExamSessionDTO.from(session),
                questionResponses,
                answerResponses,
                resultResponses,
                violationResponses,
                attempt,
                attempts
        );
    }

    private ExamAttempt getOrCreateAttempt(ExamSession session, User student) {
        Optional<ExamAttempt> optionalAttempt = attemptRepository.findBySessionIdAndStudentId(session.getId(), student.getId());
        if (optionalAttempt.isPresent()) {
            return optionalAttempt.get();
        }
        ExamAttempt attempt = new ExamAttempt();
        attempt.setSession(session);
        attempt.setStudent(student);
        attempt.setStartedAt(nowUtc());
        return attemptRepository.save(attempt);
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
