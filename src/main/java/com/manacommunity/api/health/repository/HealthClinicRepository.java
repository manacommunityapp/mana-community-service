package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthClinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthClinicRepository extends JpaRepository<HealthClinic, Long> {
    List<HealthClinic> findByProviderId(Long providerId);
}