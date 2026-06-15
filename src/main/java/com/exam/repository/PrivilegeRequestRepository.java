package com.exam.repository;

import com.exam.model.PrivilegeRequest;
import com.exam.model.PrivilegeRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrivilegeRequestRepository extends JpaRepository<PrivilegeRequest, Long> {
    List<PrivilegeRequest> findBySchoolClassId(Long classId);
    List<PrivilegeRequest> findByStatus(PrivilegeRequestStatus status);
}
