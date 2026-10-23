package com.manacommunity.api.serviceplatform.amc.repository;

import com.manacommunity.api.serviceplatform.amc.entity.AmcPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmcPlanRepository extends JpaRepository<AmcPlan, Long> {
    List<AmcPlan> findByProviderIdAndActiveTrue(Long providerId);
    List<AmcPlan> findByCategoryIdAndActiveTrue(Long categoryId);
}
