package com.manacommunity.api.release.repository;

import com.manacommunity.api.release.entity.CanaryShadowBillingDiff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CanaryShadowBillingDiffRepository extends JpaRepository<CanaryShadowBillingDiff, Long> {
    List<CanaryShadowBillingDiff> findByFlagKeyAndMatchedFalse(String flagKey);
}
