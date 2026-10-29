package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthDoctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthDoctorRepository extends JpaRepository<HealthDoctor, Long> {
    Optional<HealthDoctor> findByProviderId(Long providerId);
    Optional<HealthDoctor> findByAppUserId(Long appUserId);
    List<HealthDoctor> findByPrimarySpecialtyId(Long specialtyId);
}