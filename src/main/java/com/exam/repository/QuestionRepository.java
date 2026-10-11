package com.exam.repository;

import com.exam.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySession_Id(Long sessionId);
    List<Question> findBySession_IdOrderByOrderIndexAsc(Long sessionId);

    @Query("""
            select question.session.id, coalesce(sum(question.maxScore), 0)
            from Question question
            where question.session.id in :sessionIds
            group by question.session.id
            """)
    List<Object[]> sumMaxScoreBySessionIds(@Param("sessionIds") List<Long> sessionIds);
}
