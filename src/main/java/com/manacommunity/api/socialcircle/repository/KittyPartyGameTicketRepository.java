package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyGameTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyGameTicketRepository extends JpaRepository<KittyPartyGameTicket, Long> {

    List<KittyPartyGameTicket> findByRoomId(Long roomId);

    List<KittyPartyGameTicket> findByRoomIdAndPlayerId(Long roomId, Long playerId);

    Optional<KittyPartyGameTicket> findByRoomIdAndTicketHash(Long roomId, String ticketHash);
}
