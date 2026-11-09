package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KittyPartyDisputeRepository extends JpaRepository<KittyPartyDispute, Long> {

    List<KittyPartyDispute> findByCircleIdOrderByCreatedAtDesc(Long circleId);

    List<KittyPartyDispute> findByCycleIdOrderByCreatedAtDesc(Long cycleId);

    List<KittyPartyDispute> findByCircleIdAndStatusOrderByCreatedAtDesc(Long circleId, String status);
}
