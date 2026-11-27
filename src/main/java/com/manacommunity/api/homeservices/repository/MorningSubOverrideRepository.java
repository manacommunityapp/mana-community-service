package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.MorningSubOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MorningSubOverrideRepository extends JpaRepository<MorningSubOverride, Long> {
    Optional<MorningSubOverride> findByPlanIdAndDeliveryDate(Long planId, LocalDate deliveryDate);
}
