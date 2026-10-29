package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthAvailabilityRepository extends JpaRepository<HealthAvailability, Long> {
    List<HealthAvailability> findByDoctorIdAndIsActiveTrue(Long doctorId);
}