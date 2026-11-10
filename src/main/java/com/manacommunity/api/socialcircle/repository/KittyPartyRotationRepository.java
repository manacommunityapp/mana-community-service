package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyRotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyRotationRepository extends JpaRepository<KittyPartyRotation, Long> {

    List<KittyPartyRotation> findByCycleIdOrderByPeriodNumberAsc(Long cycleId);

    Optional<KittyPartyRotation> findByCycleIdAndPeriodNumber(Long cycleId, Integer periodNumber);

    Optional<KittyPartyRotation> findByCycleIdAndBeneficiaryUserId(Long cycleId, Long beneficiaryUserId);
}
