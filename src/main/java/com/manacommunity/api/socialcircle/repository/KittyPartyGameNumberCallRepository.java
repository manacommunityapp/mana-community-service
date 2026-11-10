package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyGameNumberCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyGameNumberCallRepository extends JpaRepository<KittyPartyGameNumberCall, Long> {

    List<KittyPartyGameNumberCall> findByRoomIdOrderBySequenceNumAsc(Long roomId);

    Optional<KittyPartyGameNumberCall> findFirstByRoomIdOrderBySequenceNumDesc(Long roomId);

    boolean existsByRoomIdAndCalledNumber(Long roomId, Integer calledNumber);

    long countByRoomId(Long roomId);
}
