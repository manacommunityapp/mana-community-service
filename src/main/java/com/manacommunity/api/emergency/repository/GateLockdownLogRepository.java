package com.manacommunity.api.emergency.repository;

import com.manacommunity.api.emergency.entity.GateLockdownLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GateLockdownLogRepository extends JpaRepository<GateLockdownLog, Long> {

    List<GateLockdownLog> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
}