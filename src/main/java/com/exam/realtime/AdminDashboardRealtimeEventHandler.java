package com.exam.realtime;

import com.exam.event.AdminDashboardRefreshRequestedEvent;
import com.exam.event.ClassRankUpdatedEvent;
import com.exam.service.AdminDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends the dashboard only after committed data can be queried afresh. */
@Component
public class AdminDashboardRealtimeEventHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardRealtimeEventHandler.class);

    private final AdminDashboardRealtimePublisher realtimePublisher;
    private final AdminDashboardService dashboardService;

    public AdminDashboardRealtimeEventHandler(
            AdminDashboardRealtimePublisher realtimePublisher,
            AdminDashboardService dashboardService
    ) {
        this.realtimePublisher = realtimePublisher;
        this.dashboardService = dashboardService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClassRankUpdated(ClassRankUpdatedEvent event) {
        publishCurrentDashboard();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDashboardRefreshRequested(AdminDashboardRefreshRequestedEvent event) {
        publishCurrentDashboard();
    }

    private void publishCurrentDashboard() {
        try {
            realtimePublisher.publish(dashboardService.getDashboardSnapshot());
        } catch (RuntimeException exception) {
            log.error("Could not publish admin dashboard realtime notification after commit", exception);
        }
    }
}
