package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyGamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyGamePlayerRepository extends JpaRepository<KittyPartyGamePlayer, Long> {

    List<KittyPartyGamePlayer> findByRoomId(Long roomId);

    Optional<KittyPartyGamePlayer> findByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomId(Long roomId);
}
