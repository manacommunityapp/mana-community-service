package com.manacommunity.api.events.repository;

import com.manacommunity.api.events.entity.ActivityRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRegistrationRepository extends JpaRepository<ActivityRegistration, Long> {

    List<ActivityRegistration> findByProgramIdOrderByRegisteredAtDesc(Long programId);

    List<ActivityRegistration> findByUserIdOrderByRegisteredAtDesc(Long userId);

    Optional<ActivityRegistration> findByProgramIdAndUserId(Long programId, Long userId);

    boolean existsByProgramIdAndUserId(Long programId, Long userId);

    int countByProgramIdAndStatus(Long programId, ActivityRegistration.ActivityRegStatus status);
}
