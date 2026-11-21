package com.manacommunity.api.sportsops.repository;

import com.manacommunity.api.sportsops.entity.SportsNoShowDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SportsNoShowDepositRepository extends JpaRepository<SportsNoShowDeposit, Long> {
    List<SportsNoShowDeposit> findByCommunityId(Long communityId);
    Optional<SportsNoShowDeposit> findByCommunityIdAndBookingId(Long communityId, Long bookingId);
    List<SportsNoShowDeposit> findByCommunityIdAndUserId(Long communityId, Long userId);
}
