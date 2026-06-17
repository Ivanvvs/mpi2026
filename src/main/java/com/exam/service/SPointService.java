package com.exam.service;

import com.exam.model.ExamResult;
import com.exam.model.ExamSession;
import com.exam.model.SPointTransaction;
import com.exam.model.SchoolClass;
import com.exam.model.User;
import com.exam.repository.ExamResultRepository;
import com.exam.repository.SPointTransactionRepository;
import com.exam.repository.SchoolClassRepository;
import com.exam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SPointService {

    private static final String EXAM_RESULT_REASON = "EXAM_RESULT";

    private final UserRepository userRepository;
    private final SchoolClassRepository classRepository;
    private final ExamResultRepository resultRepository;
    private final SPointTransactionRepository transactionRepository;

    public SPointService(
            UserRepository userRepository,
            SchoolClassRepository classRepository,
            ExamResultRepository resultRepository,
            SPointTransactionRepository transactionRepository
    ) {
        this.userRepository = userRepository;
        this.classRepository = classRepository;
        this.resultRepository = resultRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void applyExamResults(ExamSession session, List<ExamResult> results) {
        for (ExamResult result : results) {
            applyStudentResult(result, session);
        }

        if (session.getSchoolClass() != null) {
            recalculateClassBalance(session.getSchoolClass());
        }
    }

    @Transactional
    public int recalculateClassBalance(SchoolClass schoolClass) {
        int balance = userRepository.sumSPointsBySchoolClassId(schoolClass.getId());
        schoolClass.setsPoints(balance);
        classRepository.save(schoolClass);
        return balance;
    }

    private void applyStudentResult(ExamResult result, ExamSession session) {
        User student = result.getStudent();
        if (student == null || student.getSchoolClass() == null) {
            return;
        }

        int nextAwardedPoints = result.getFinalScore();
        int delta = nextAwardedPoints - result.getAwardedSPoints();
        if (delta == 0) {
            return;
        }

        student.setsPoints(Math.max(0, student.getsPoints() + delta));
        userRepository.save(student);

        result.setAwardedSPoints(nextAwardedPoints);
        resultRepository.save(result);

        SPointTransaction transaction = new SPointTransaction();
        transaction.setUser(student);
        transaction.setSchoolClass(student.getSchoolClass());
        transaction.setSession(session);
        transaction.setPointsDelta(delta);
        transaction.setResultingBalance(student.getsPoints());
        transaction.setReason(EXAM_RESULT_REASON);
        transactionRepository.save(transaction);
    }
}
