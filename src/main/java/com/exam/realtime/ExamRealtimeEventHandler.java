package com.exam.realtime;

import com.exam.event.ExamAnswerSavedEvent;
import com.exam.event.ExamAttemptSubmittedEvent;
import com.exam.event.ExamFinishedEvent;
import com.exam.event.ExamGradedEvent;
import com.exam.event.ExamStartedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Translates committed application events to the existing exam STOMP payloads. */
@Component
public class ExamRealtimeEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ExamRealtimeEventHandler.class);

    private final ExamRealtimePublisher realtimePublisher;

    public ExamRealtimeEventHandler(ExamRealtimePublisher realtimePublisher) {
        this.realtimePublisher = realtimePublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamStarted(ExamStartedEvent event) {
        publishSafely("EXAM_STARTED", event.examId(),
                () -> realtimePublisher.publish(event.examId(), "EXAM_STARTED", "Exam has been started"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamFinished(ExamFinishedEvent event) {
        publishSafely("EXAM_FINISHED", event.examId(),
                () -> realtimePublisher.publish(event.examId(), "EXAM_FINISHED", event.message()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamGraded(ExamGradedEvent event) {
        publishSafely("EXAM_GRADED", event.examId(),
                () -> realtimePublisher.publish(event.examId(), "EXAM_GRADED", "Exam results have been saved"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerSaved(ExamAnswerSavedEvent event) {
        publishSafely("ANSWER_SAVED", event.examId(),
                () -> realtimePublisher.publish(event.examId(), event.userId(), "ANSWER_SAVED", "Student answer has been saved"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttemptSubmitted(ExamAttemptSubmittedEvent event) {
        publishSafely("ATTEMPT_SUBMITTED", event.examId(),
                () -> realtimePublisher.publish(event.examId(), event.userId(), "ATTEMPT_SUBMITTED", "Student attempt has been submitted"));
    }

    private void publishSafely(String eventType, Long examId, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            log.error("Could not publish {} realtime notification for exam {} after commit", eventType, examId, exception);
        }
    }
}
