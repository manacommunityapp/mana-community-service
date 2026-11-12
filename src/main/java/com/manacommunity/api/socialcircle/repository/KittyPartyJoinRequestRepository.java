package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyJoinRequestRepository extends JpaRepository<KittyPartyJoinRequest, Long> {

    List<KittyPartyJoinRequest> findByCircleIdAndStatus(Long circleId, String status);

    Optional<KittyPartyJoinRequest> findByCircleIdAndUserId(Long circleId, Long userId);
}
