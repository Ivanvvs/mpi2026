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

import static com.exam.util.DateTimeUtils.nowUtc;

@Service
public class ExamLifecycleService {

    private final ExamSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final QuestionRepository questionRepository;
    private final ExamRealtimePublisher realtimePublisher;
    private final CurrentUserService currentUserService;
    private final AccessControlService accessControl;

    public ExamLifecycleService(
            ExamSessionRepository sessionRepository,
            SchoolClassRepository classRepository,
            QuestionRepository questionRepository,
            ExamRealtimePublisher realtimePublisher,
            CurrentUserService currentUserService,
            AccessControlService accessControl
    ) {
        this.sessionRepository = sessionRepository;
        this.classRepository = classRepository;
        this.questionRepository = questionRepository;
        this.realtimePublisher = realtimePublisher;
        this.currentUserService = currentUserService;
        this.accessControl = accessControl;
    }

    @Transactional
    public ExamSession createExam(CreateExamRequest request) {
        assertCanCreateExam();

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
        assertCanManageExam(session);
        session.setStartTime(nowUtc());
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
        assertCanManageExam(session);
        session.setEndTime(nowUtc());
        ExamSession saved = sessionRepository.save(session);
        realtimePublisher.publish(id, "EXAM_FINISHED", "Exam has been finished");
        return saved;
    }

    public ExamSession getSession(Long id) {
        ExamSession session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam was not found"));
        assertCanViewExam(session);
        return finishIfExpired(session);
    }

    public List<ExamSession> getExams() {
        return sessionRepository.findAll().stream()
                .map(this::finishIfExpired)
                .toList();
    }

    public List<ExamSession> getExamsForCurrentUser() {
        User user = accessControl.currentProfile();
        Role role = accessControl.currentRole();
        if (role == Role.ADMIN) {
            return getExams();
        }
        if (role == Role.EXAMINER) {
            return sessionRepository.findByCreatedById(accessControl.currentAccountId()).stream()
                    .map(this::finishIfExpired)
                    .toList();
        }
        if (user.getSchoolClass() == null) {
            return List.of();
        }
        return sessionRepository.findBySchoolClassId(user.getSchoolClass().getId()).stream()
                .map(this::finishIfExpired)
                .toList();
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

        LocalDateTime startedAt = session.getScheduledStartTime() == null ? session.getStartTime() : session.getScheduledStartTime();
        if (startedAt == null) {
            return session;
        }

        LocalDateTime endsAt = startedAt.plusMinutes(session.getDurationMinutes());
        if (nowUtc().isBefore(endsAt)) {
            return session;
        }

        session.setStatus(ExamStatus.FINISHED);
        session.setEndTime(endsAt);
        ExamSession saved = sessionRepository.save(session);
        realtimePublisher.publish(session.getId(), "EXAM_FINISHED", "Exam has been finished by time limit");
        return saved;
    }

    private void assertCanViewExam(ExamSession session) {
        Role role = accessControl.currentRole();
        boolean allowed = role == Role.ADMIN
                || (role == Role.EXAMINER && accessControl.owns(session.getCreatedBy()))
                || ((role == Role.STUDENT || role == Role.CURATOR)
                && accessControl.belongsToClass(session.getSchoolClass()));
        accessControl.require(allowed, "Current user cannot access this exam");
    }

    private void assertCanManageExam(ExamSession session) {
        accessControl.require(
                accessControl.hasRole(Role.EXAMINER) && accessControl.owns(session.getCreatedBy()),
                "Current user cannot manage this exam"
        );
    }

    private void assertCanCreateExam() {
        accessControl.require(
                accessControl.hasRole(Role.EXAMINER),
                "Current user cannot create exam"
        );
    }
}
