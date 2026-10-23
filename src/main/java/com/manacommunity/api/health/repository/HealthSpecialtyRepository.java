package com.manacommunity.api.health.repository;

import com.manacommunity.api.health.entity.HealthSpecialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthSpecialtyRepository extends JpaRepository<HealthSpecialty, Long> {
    List<HealthSpecialty> findByIsActiveTrueOrderBySortOrderAsc();
    Optional<HealthSpecialty> findByCode(String code);
}