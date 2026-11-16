package com.manacommunity.api.guard.repository;

import com.manacommunity.api.guard.entity.GuardSos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuardSosRepository extends JpaRepository<GuardSos, Long> {
    List<GuardSos> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}
