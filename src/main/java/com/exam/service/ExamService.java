package com.exam.service;

import com.exam.auth.AppUser;
import com.exam.auth.AppUserRepository;
import com.exam.auth.Role;
import com.exam.dto.CreateExamRequest;
import com.exam.dto.ExamDashboardResponse;
import com.exam.dto.ExamDetailsResponse;
import com.exam.dto.GradeExamRequest;
import com.exam.dto.QuestionRequest;
import com.exam.dto.StudentScoreRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.Answer;
import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import com.exam.model.Question;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.AnswerRepository;
import com.exam.repository.ExamResultRepository;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import com.exam.realtime.ExamRealtimePublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ExamService {

    private final ExamSessionRepository repository;
    private final SchoolClassRepository classRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final ExamResultRepository resultRepository;
    private final ExamRealtimePublisher realtimePublisher;
    private final AppUserRepository appUserRepository;

    public ExamService(
            ExamSessionRepository repository,
            SchoolClassRepository classRepository,
            QuestionRepository questionRepository,
            AnswerRepository answerRepository,
            UserRepository userRepository,
            ExamResultRepository resultRepository,
            ExamRealtimePublisher realtimePublisher,
            AppUserRepository appUserRepository
    ) {
        this.repository = repository;
        this.classRepository = classRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.resultRepository = resultRepository;
        this.realtimePublisher = realtimePublisher;
        this.appUserRepository = appUserRepository;
    }

    @Transactional
    public ExamSession createSession(ExamSession session) {
        session.setStatus(ExamStatus.PREPARED);
        return repository.save(session);
    }

    @Transactional
    public ExamSession createExam(CreateExamRequest request) {
        if (request.getQuestions().isEmpty()) {
            throw new BadRequestException("Exam must contain at least one question");
        }

        SchoolClass schoolClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class was not found"));

        ExamSession session = new ExamSession();
        session.setTitle(request.getTitle());
        session.setSubject(request.getSubject());
        session.setSchoolClass(schoolClass);
        session.setScheduledStartTime(request.getScheduledStartTime());
        session.setDurationMinutes(request.getDurationMinutes());
        session.setDescription(request.getDescription());
        session.setTotalQuestions(request.getQuestions().size());
        session.setStatus(ExamStatus.PREPARED);
        session.setCreatedBy(currentUser());
        session = repository.save(session);

        int index = 1;
        for (QuestionRequest questionRequest : request.getQuestions()) {
            Question question = new Question();
            question.setSessionId(session.getId());
            question.setOrderIndex(questionRequest.getOrderIndex() == null ? index : questionRequest.getOrderIndex());
            question.setText(questionRequest.getText());
            question.setType(questionRequest.getType());
            question.setOptionsJson(questionRequest.getOptionsJson());
            question.setCorrectAnswer(questionRequest.getCorrectAnswer());
            question.setMaxScore(questionRequest.getMaxScore());
            question.setMetadataJson(questionRequest.getMetadataJson());
            questionRepository.save(question);
            index++;
        }

        return session;
    }

    @Transactional
    public ExamSession startSession(Long id) {
        ExamSession session = getSession(id);

        if (session.getStatus() == ExamStatus.FINISHED) {
            throw new BadRequestException("Finished exam cannot be started");
        }

        session.setStatus(ExamStatus.ACTIVE);
        session.setStartTime(LocalDateTime.now());
        ExamSession saved = repository.save(session);
        realtimePublisher.publish(id, "EXAM_STARTED", "Exam has been started");
        return saved;
    }

    @Transactional
    public ExamSession endSession(Long id) {
        ExamSession session = getSession(id);

        if (session.getStatus() != ExamStatus.ACTIVE) {
            throw new BadRequestException("Only active exam can be finished");
        }

        session.setStatus(ExamStatus.FINISHED);
        session.setEndTime(LocalDateTime.now());
        ExamSession saved = repository.save(session);
        realtimePublisher.publish(id, "EXAM_FINISHED", "Exam has been finished");
        return saved;
    }

    @Transactional
    public Answer saveAnswer(Long sessionId, Long studentId, Long questionId, String text, boolean finalSubmitted) {
        ExamSession session = getSession(sessionId);

        if (session.getStatus() != ExamStatus.ACTIVE) {
            throw new BadRequestException("Answers can be saved only for active exam");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student was not found"));
        validateStudentCanTakeExam(session, student);

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
        answer.setSavedAt(LocalDateTime.now());
        answer.setFinalSubmitted(finalSubmitted);

        Answer saved = answerRepository.save(answer);
        realtimePublisher.publish(sessionId, "ANSWER_SAVED", "Student answer has been saved");
        return saved;
    }

    @Transactional
    public List<ExamResult> gradeExam(Long sessionId, GradeExamRequest request) {
        ExamSession session = getSession(sessionId);

        if (session.getStatus() != ExamStatus.FINISHED) {
            throw new BadRequestException("Exam must be finished before grading");
        }

        for (StudentScoreRequest scoreRequest : request.getScores()) {
            User student = userRepository.findById(scoreRequest.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student was not found"));

            ExamResult result = resultRepository.findBySessionIdAndStudentId(sessionId, student.getId())
                    .orElseGet(ExamResult::new);
            result.setSession(session);
            result.setStudent(student);
            result.setRawScore(scoreRequest.getRawScore());
            result.setViolationPenalty(0);
            result.setFinalScore(scoreRequest.getRawScore());
            result.setGradedAt(LocalDateTime.now());
            resultRepository.save(result);
        }

        List<ExamResult> results = resultRepository.findBySessionId(sessionId).stream()
                .sorted(Comparator.comparingInt(ExamResult::getFinalScore).reversed())
                .toList();

        int place = 1;
        int totalScore = 0;
        for (ExamResult result : results) {
            result.setRankPlace(place++);
            totalScore += result.getFinalScore();
            resultRepository.save(result);
        }

        if (session.getSchoolClass() != null && !results.isEmpty()) {
            SchoolClass schoolClass = session.getSchoolClass();
            int averageScore = totalScore / results.size();
            schoolClass.setsPoints(schoolClass.getsPoints() + averageScore);
            classRepository.save(schoolClass);
        }

        realtimePublisher.publish(sessionId, "EXAM_GRADED", "Exam results have been saved");
        return resultRepository.findBySessionId(sessionId);
    }

    public ExamSession getSession(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam was not found"));
    }

    public List<Question> getQuestions(Long sessionId) {
        return questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
    }

    public List<ExamSession> getExams() {
        return repository.findAll();
    }

    public List<ExamSession> getExamsForClass(Long classId) {
        return repository.findBySchoolClassId(classId);
    }

    public List<Answer> getAnswers(Long sessionId) {
        return answerRepository.findBySessionId(sessionId);
    }

    public List<Answer> getStudentAnswers(Long sessionId, Long studentId) {
        return answerRepository.findBySessionIdAndUserId(sessionId, studentId);
    }

    public List<ExamResult> getResults(Long sessionId) {
        return resultRepository.findBySessionId(sessionId);
    }

    public List<ExamResult> getStudentResults(Long studentId) {
        return resultRepository.findByStudentId(studentId);
    }

    public ExamDetailsResponse getDetails(Long sessionId) {
        ExamSession session = getSession(sessionId);
        return new ExamDetailsResponse(
                session,
                getQuestions(sessionId),
                getAnswers(sessionId),
                getResults(sessionId)
        );
    }

    public ExamDashboardResponse getDashboard() {
        return new ExamDashboardResponse(repository.findAll(), classRepository.findByActiveTrueOrderBySPointsDesc());
    }

    private AppUser currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private void validateStudentCanTakeExam(ExamSession session, User student) {
        if (!student.isActive()) {
            throw new BadRequestException("Inactive student cannot take exam");
        }

        if (student.getAccount() == null || student.getAccount().getRole() != Role.STUDENT) {
            throw new BadRequestException("Only students can submit exam answers");
        }

        if (session.getSchoolClass() != null) {
            if (student.getSchoolClass() == null || !session.getSchoolClass().getId().equals(student.getSchoolClass().getId())) {
                throw new BadRequestException("Student does not belong to exam class");
            }
        }
    }
}
