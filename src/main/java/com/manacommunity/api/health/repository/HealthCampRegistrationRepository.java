package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthCampRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthCampRegistrationRepository extends JpaRepository<HealthCampRegistration, Long> {
    List<HealthCampRegistration> findByCampId(Long campId);
    List<HealthCampRegistration> findByUserId(Long userId);
}