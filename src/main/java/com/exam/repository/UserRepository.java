package com.exam.repository;

import com.exam.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAccountId(Long accountId);
    List<User> findBySchoolClassIdAndActiveTrue(Long classId);
    List<User> findByActiveTrue();

    @Query("select coalesce(sum(user.sPoints), 0) from User user where user.schoolClass.id = :classId and user.active = true")
    int sumSPointsBySchoolClassId(Long classId);
}
