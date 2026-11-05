package com.manacommunity.api.wellness.repository;

import com.manacommunity.api.wellness.entity.UserProgramEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserProgramEnrollmentRepository extends JpaRepository<UserProgramEnrollment, Long> {
    List<UserProgramEnrollment> findByUserIdOrderByEnrolledAtDesc(Long userId);
    Optional<UserProgramEnrollment> findByUserIdAndProgramId(Long userId, Long programId);
    Optional<UserProgramEnrollment> findFirstByUserIdAndStatusOrderByEnrolledAtDesc(Long userId, String status);
}
