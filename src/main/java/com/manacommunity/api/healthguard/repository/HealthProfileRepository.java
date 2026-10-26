package com.manacommunity.api.healthguard.repository;

import com.manacommunity.api.healthguard.entity.HealthProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HealthProfileRepository extends JpaRepository<HealthProfile, Long> {
    Optional<HealthProfile> findByUserId(Long userId);
}
