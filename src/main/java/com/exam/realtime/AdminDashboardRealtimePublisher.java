package com.exam.realtime;

import com.exam.dto.AdminDashboardResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class AdminDashboardRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public AdminDashboardRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(AdminDashboardResponse response) {
        messagingTemplate.convertAndSend("/topic/admin/dashboard", response);
    }
}
