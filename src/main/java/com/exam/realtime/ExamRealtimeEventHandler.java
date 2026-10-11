package com.exam.realtime;

import com.exam.event.ExamAnswerSavedEvent;
import com.exam.event.ExamAttemptSubmittedEvent;
import com.exam.event.ExamFinishedEvent;
import com.exam.event.ExamGradedEvent;
import com.exam.event.ExamStartedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Translates committed application events to the existing exam STOMP payloads. */
@Component
public class ExamRealtimeEventHandler {

    private final ExamRealtimePublisher realtimePublisher;

    public ExamRealtimeEventHandler(ExamRealtimePublisher realtimePublisher) {
        this.realtimePublisher = realtimePublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamStarted(ExamStartedEvent event) {
        realtimePublisher.publish(event.examId(), "EXAM_STARTED", "Exam has been started");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamFinished(ExamFinishedEvent event) {
        realtimePublisher.publish(event.examId(), "EXAM_FINISHED", event.message());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExamGraded(ExamGradedEvent event) {
        realtimePublisher.publish(event.examId(), "EXAM_GRADED", "Exam results have been saved");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnswerSaved(ExamAnswerSavedEvent event) {
        realtimePublisher.publish(event.examId(), event.userId(), "ANSWER_SAVED", "Student answer has been saved");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAttemptSubmitted(ExamAttemptSubmittedEvent event) {
        realtimePublisher.publish(event.examId(), event.userId(), "ATTEMPT_SUBMITTED", "Student attempt has been submitted");
    }
}
