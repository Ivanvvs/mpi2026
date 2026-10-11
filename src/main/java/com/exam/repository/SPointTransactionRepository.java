package com.exam.repository;

import com.exam.model.SPointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SPointTransactionRepository extends JpaRepository<SPointTransaction, Long> {

    @Query("""
            select transaction from SPointTransaction transaction
            left join fetch transaction.session
            where transaction.user.id = :userId and transaction.schoolClass.id = :classId
            order by transaction.createdAt desc, transaction.id desc
            """)
    List<SPointTransaction> findHistoryByUserIdAndClassId(@Param("userId") Long userId, @Param("classId") Long classId);
}
