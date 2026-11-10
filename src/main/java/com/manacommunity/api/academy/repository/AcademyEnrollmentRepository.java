package com.manacommunity.api.academy.repository;

import com.manacommunity.api.academy.entity.AcademyEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademyEnrollmentRepository extends JpaRepository<AcademyEnrollment, Long> {
    List<AcademyEnrollment> findByUserIdOrderByEnrolledAtDesc(Long userId);
    Optional<AcademyEnrollment> findByProgramIdAndUserId(Long programId, Long userId);
    long countByProgramId(Long programId);
}
