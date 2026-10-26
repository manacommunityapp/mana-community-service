package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthRegistrationRepository extends JpaRepository<HealthRegistration, Long> {
    List<HealthRegistration> findByDoctorId(Long doctorId);
}