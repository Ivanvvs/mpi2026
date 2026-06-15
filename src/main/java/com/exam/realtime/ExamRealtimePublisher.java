package com.exam.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExamRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public ExamRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(Long examId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/exams/" + examId,
                new ExamRealtimeEvent(examId, type, message)
        );
    }
}
