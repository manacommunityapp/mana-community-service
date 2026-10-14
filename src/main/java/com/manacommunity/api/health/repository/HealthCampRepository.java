package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthCamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HealthCampRepository extends JpaRepository<HealthCamp, Long> {
    List<HealthCamp> findByTenantIdAndCampDateGreaterThanEqualOrderByCampDateAsc(Long tenantId, LocalDate date);
}