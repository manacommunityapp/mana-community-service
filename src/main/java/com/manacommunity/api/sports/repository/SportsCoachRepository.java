package com.manacommunity.api.sports.repository;

import com.manacommunity.api.sports.model.SportsCoach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SportsCoachRepository extends JpaRepository<SportsCoach, Long> {

    List<SportsCoach> findByIsActiveTrue();

    List<SportsCoach> findByVendorIdAndIsActiveTrue(Long vendorId);

    List<SportsCoach> findByCommunityIdAndIsActiveTrue(Long communityId);
}
