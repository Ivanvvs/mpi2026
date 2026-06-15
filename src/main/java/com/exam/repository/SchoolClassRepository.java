package com.exam.repository;

import com.exam.model.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    Optional<SchoolClass> findByName(String name);
    boolean existsByName(String name);

    @Query("select schoolClass from SchoolClass schoolClass where schoolClass.active = true order by schoolClass.sPoints desc")
    List<SchoolClass> findByActiveTrueOrderBySPointsDesc();
}
