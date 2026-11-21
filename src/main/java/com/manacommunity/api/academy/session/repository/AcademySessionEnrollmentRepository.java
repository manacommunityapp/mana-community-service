package com.manacommunity.api.academy.session.repository;

import com.manacommunity.api.academy.session.entity.AcademySessionEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AcademySessionEnrollmentRepository extends JpaRepository<AcademySessionEnrollment, Long> {
    Optional<AcademySessionEnrollment> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<AcademySessionEnrollment> findByStudentIdOrderByEnrolledAtDesc(Long studentId);
}
