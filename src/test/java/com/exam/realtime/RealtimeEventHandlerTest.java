package com.exam.realtime;

import com.exam.dto.AdminDashboardResponse;
import com.exam.event.AdminDashboardRefreshRequestedEvent;
import com.exam.event.ExamStartedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;

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

        TrackingExamRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
            super(messagingTemplate);
        }

        @Override
        public void publish(Long examId, String type, String message) {
            count++;
            lastType = type;
        }

        void reset() {
            count = 0;
            lastType = null;
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
