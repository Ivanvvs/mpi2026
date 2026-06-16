package com.exam.service;

import com.exam.auth.Role;
import com.exam.dto.CreateExamRequest;
import com.exam.exception.BadRequestException;
import com.exam.exception.ResourceNotFoundException;
import com.exam.model.ExamSession;
import com.exam.model.ExamStatus;
import com.exam.model.Question;
import com.exam.model.QuestionType;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.ExamSessionRepository;
import com.exam.repository.QuestionRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.realtime.ExamRealtimePublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ExamLifecycleService {

    private final ExamSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final QuestionRepository questionRepository;
    private final ExamRealtimePublisher realtimePublisher;
    private final CurrentUserService currentUserService;

    public ExamLifecycleService(
            ExamSessionRepository sessionRepository,
            SchoolClassRepository classRepository,
            QuestionRepository questionRepository,
            ExamRealtimePublisher realtimePublisher,
            CurrentUserService currentUserService
    ) {
        this.sessionRepository = sessionRepository;
        this.classRepository = classRepository;
        this.questionRepository = questionRepository;
        this.realtimePublisher = realtimePublisher;
        this.currentUserService = currentUserService;
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
        session.setCreatedBy(currentUserService.getAccount());
        session = sessionRepository.save(session);

        int index = 1;
        for (var questionRequest : request.getQuestions()) {
            Question question = new Question();
            question.setSessionId(session.getId());
            question.setOrderIndex(questionRequest.getOrderIndex() == null ? index : questionRequest.getOrderIndex());
            question.setText(questionRequest.getText());
            question.setType(questionRequest.getType() == null ? QuestionType.TEXT : questionRequest.getType());
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
        ExamSession saved = sessionRepository.save(session);
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
        ExamSession saved = sessionRepository.save(session);
        realtimePublisher.publish(id, "EXAM_FINISHED", "Exam has been finished");
        return saved;
    }

    public ExamSession getSession(Long id) {
        ExamSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam was not found"));
        return finishIfExpired(session);
    }

    public List<ExamSession> getExams() {
        return sessionRepository.findAll().stream()
                .map(this::finishIfExpired)
                .toList();
    }

    public List<ExamSession> getExamsForCurrentUser() {
        User user = currentUserService.getProfile();
        if (user.getAccount() != null && user.getAccount().getRole() == Role.STUDENT) {
            if (user.getSchoolClass() == null) {
                return List.of();
            }
            return sessionRepository.findBySchoolClassId(user.getSchoolClass().getId()).stream()
                    .map(this::finishIfExpired)
                    .toList();
        }
        return getExams();
    }

    public List<ExamSession> getExamsForClass(Long classId) {
        return sessionRepository.findBySchoolClassId(classId).stream()
                .map(this::finishIfExpired)
                .toList();
    }

    @Transactional
    public void finishExpiredExams() {
        sessionRepository.findByStatus(ExamStatus.ACTIVE).forEach(this::finishIfExpired);
    }

    private ExamSession finishIfExpired(ExamSession session) {
        if (session.getStatus() != ExamStatus.ACTIVE || session.getDurationMinutes() == null) {
            return session;
        }

        LocalDateTime startedAt = session.getStartTime() == null ? session.getScheduledStartTime() : session.getStartTime();
        if (startedAt == null) {
            return session;
        }

        LocalDateTime endsAt = startedAt.plusMinutes(session.getDurationMinutes());
        if (LocalDateTime.now().isBefore(endsAt)) {
            return session;
        }

        session.setStatus(ExamStatus.FINISHED);
        session.setEndTime(endsAt);
        ExamSession saved = sessionRepository.save(session);
        realtimePublisher.publish(session.getId(), "EXAM_FINISHED", "Exam has been finished by time limit");
        return saved;
    }
}
