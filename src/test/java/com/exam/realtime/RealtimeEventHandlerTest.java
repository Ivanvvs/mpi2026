package com.exam.realtime;

import com.exam.dto.AdminDashboardResponse;
import com.exam.event.AdminDashboardRefreshRequestedEvent;
import com.exam.event.ClassRankUpdatedEvent;
import com.exam.event.ExamAnswerSavedEvent;
import com.exam.event.ExamAttemptSubmittedEvent;
import com.exam.event.ExamFinishedEvent;
import com.exam.event.ExamGradedEvent;
import com.exam.event.ExamStartedEvent;
import com.exam.service.AdminDashboardService;
import com.exam.model.SchoolClass;
import com.exam.repository.SchoolClassRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {"debug=false", "logging.level.root=warn"})
@ActiveProfiles("test")
@Import(RealtimeEventHandlerTest.TrackingPublishersConfig.class)
class RealtimeEventHandlerTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private TrackingExamRealtimePublisher examPublisher;
    @Autowired
    private TrackingAdminDashboardRealtimePublisher dashboardPublisher;
    @Autowired
    private AdminDashboardService dashboardService;
    @Autowired
    private SchoolClassRepository classRepository;

    @BeforeEach
    void resetPublishers() {
        examPublisher.reset();
        dashboardPublisher.reset();
    }

    @Test
    void deliversRealtimeNotificationsOnlyAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new ExamStartedEvent(42L));
            eventPublisher.publishEvent(new AdminDashboardRefreshRequestedEvent());
            assertThat(examPublisher.count).isZero();
            assertThat(dashboardPublisher.count).isZero();
        });

        assertThat(examPublisher.count).isEqualTo(1);
        assertThat(examPublisher.lastType).isEqualTo("EXAM_STARTED");
        assertThat(dashboardPublisher.count).isEqualTo(1);
        assertThat(dashboardPublisher.lastResponse).isNotNull();
    }

    @Test
    void doesNotDeliverRealtimeNotificationsAfterRollback() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new ExamStartedEvent(42L));
            eventPublisher.publishEvent(new AdminDashboardRefreshRequestedEvent());
            status.setRollbackOnly();
        });

        assertThat(examPublisher.count).isZero();
        assertThat(dashboardPublisher.count).isZero();
    }

    @Test
    void translatesEveryExamAndDashboardEventAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new ExamStartedEvent(42L));
            eventPublisher.publishEvent(new ExamFinishedEvent(42L, "finished"));
            eventPublisher.publishEvent(new ExamGradedEvent(42L));
            eventPublisher.publishEvent(new ExamAnswerSavedEvent(42L, 7L));
            eventPublisher.publishEvent(new ExamAttemptSubmittedEvent(42L, 7L));
            eventPublisher.publishEvent(new ClassRankUpdatedEvent(List.of(1L)));
        });

        assertThat(examPublisher.count).isEqualTo(5);
        assertThat(examPublisher.types).containsExactly(
                "EXAM_STARTED", "EXAM_FINISHED", "EXAM_GRADED", "ANSWER_SAVED", "ATTEMPT_SUBMITTED"
        );
        assertThat(dashboardPublisher.count).isEqualTo(1);
    }

    @Test
    void websocketFailureAfterCommitDoesNotFailTheCommittedTransaction() {
        examPublisher.failOnPublish = true;

        assertThatCode(() -> transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new ExamStartedEvent(42L))
        )).doesNotThrowAnyException();

        assertThat(examPublisher.count).isEqualTo(1);
    }

    @Test
    void dashboardSnapshotIsCalledThroughTransactionalSpringProxy() throws NoSuchMethodException {
        var transactional = AdminDashboardService.class
                .getMethod("getDashboardSnapshot")
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertThat(AopUtils.isAopProxy(dashboardService)).isTrue();
        assertThat(transactional.readOnly()).isTrue();
        assertThat(transactional.propagation())
                .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRES_NEW);
    }

    @Test
    void dashboardNotificationContainsStateCommittedBeforeTheEventListenerRuns() {
        SchoolClass schoolClass = classRepository.findByActiveTrueOrderBySPointsDesc().getFirst();
        int committedPoints = schoolClass.getsPoints() + 17;

        transactionTemplate.executeWithoutResult(status -> {
            SchoolClass managedClass = classRepository.findById(schoolClass.getId()).orElseThrow();
            managedClass.setsPoints(committedPoints);
            classRepository.save(managedClass);
            eventPublisher.publishEvent(new AdminDashboardRefreshRequestedEvent());
        });

        assertThat(dashboardPublisher.lastResponse.getClasses())
                .filteredOn(item -> item.getId().equals(schoolClass.getId()))
                .singleElement()
                .extracting(item -> item.getsPoints())
                .isEqualTo(committedPoints);
    }

    @TestConfiguration
    static class TrackingPublishersConfig {
        @Bean
        @Primary
        TrackingExamRealtimePublisher trackingExamRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            return new TrackingExamRealtimePublisher(messagingTemplate);
        }

        @Bean
        @Primary
        TrackingAdminDashboardRealtimePublisher trackingAdminDashboardRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            return new TrackingAdminDashboardRealtimePublisher(messagingTemplate);
        }
    }

    static class TrackingExamRealtimePublisher extends ExamRealtimePublisher {
        private int count;
        private String lastType;
        private final List<String> types = new java.util.ArrayList<>();
        private boolean failOnPublish;

        TrackingExamRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            super(messagingTemplate);
        }

        @Override
        public void publish(Long examId, String type, String message) {
            count++;
            lastType = type;
            types.add(type);
            if (failOnPublish) {
                throw new IllegalStateException("simulated STOMP failure");
            }
        }

        @Override
        public void publish(Long examId, Long userId, String type, String message) {
            publish(examId, type, message);
        }

        void reset() {
            count = 0;
            lastType = null;
            types.clear();
            failOnPublish = false;
        }
    }

    static class TrackingAdminDashboardRealtimePublisher extends AdminDashboardRealtimePublisher {
        private int count;
        private AdminDashboardResponse lastResponse;

        TrackingAdminDashboardRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            super(messagingTemplate);
        }

        @Override
        public void publish(AdminDashboardResponse response) {
            count++;
            lastResponse = response;
        }

        void reset() {
            count = 0;
            lastResponse = null;
        }
    }
}
