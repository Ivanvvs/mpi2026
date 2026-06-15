package com.exam.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpirationScheduler {

    private final ExamService examService;
    private final VotingService votingService;

    public ExpirationScheduler(ExamService examService, VotingService votingService) {
        this.examService = examService;
        this.votingService = votingService;
    }

    @Scheduled(fixedDelay = 30000)
    public void closeExpiredSessions() {
        examService.finishExpiredExams();
        votingService.finishExpiredVotings();
    }
}
