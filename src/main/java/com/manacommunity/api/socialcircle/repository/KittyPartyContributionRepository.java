package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyContributionRepository extends JpaRepository<KittyPartyContribution, Long> {

    List<KittyPartyContribution> findByCycleIdOrderByPeriodNumberAsc(Long cycleId);

    List<KittyPartyContribution> findByCycleIdAndPeriodNumber(Long cycleId, Integer periodNumber);

    Optional<KittyPartyContribution> findByCycleIdAndMemberUserIdAndPeriodNumber(Long cycleId, Long memberUserId, Integer periodNumber);
}
