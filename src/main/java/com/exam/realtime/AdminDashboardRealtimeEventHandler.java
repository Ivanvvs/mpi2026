package com.exam.realtime;

import com.exam.event.AdminDashboardRefreshRequestedEvent;
import com.exam.event.ClassRankUpdatedEvent;
import com.exam.service.AdminDashboardService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Sends the dashboard only after committed data can be queried afresh. */
@Component
public class AdminDashboardRealtimeEventHandler {

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
        realtimePublisher.publish(dashboardService.getDashboardSnapshot());
    }
}
