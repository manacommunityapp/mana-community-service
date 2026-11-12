package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsBatchEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportsBatchEnrollmentRepository extends JpaRepository<SportsBatchEnrollment, Long> {

    List<SportsBatchEnrollment> findByBatchId(Long batchId);

    List<SportsBatchEnrollment> findByStudentId(Long studentId);

    Optional<SportsBatchEnrollment> findByBatchIdAndStudentId(Long batchId, Long studentId);
}
