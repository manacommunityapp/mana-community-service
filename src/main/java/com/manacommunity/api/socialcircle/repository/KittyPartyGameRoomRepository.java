package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyGameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyGameRoomRepository extends JpaRepository<KittyPartyGameRoom, Long> {

    List<KittyPartyGameRoom> findByCircleIdOrderByCreatedAtDesc(Long circleId);

    Optional<KittyPartyGameRoom> findFirstByCircleIdAndStatusOrderByCreatedAtDesc(Long circleId, String status);
}
