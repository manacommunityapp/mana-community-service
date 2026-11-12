package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyGameWinner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyGameWinnerRepository extends JpaRepository<KittyPartyGameWinner, Long> {

    List<KittyPartyGameWinner> findByRoomIdOrderByClaimedAtAsc(Long roomId);

    List<KittyPartyGameWinner> findByRoomIdIn(List<Long> roomIds);

    Optional<KittyPartyGameWinner> findByRoomIdAndClaimType(Long roomId, String claimType);

    boolean existsByRoomIdAndClaimType(Long roomId, String claimType);
}
