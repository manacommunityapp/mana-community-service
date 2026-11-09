package com.manacommunity.api.socialcircle.repository;

import com.manacommunity.api.socialcircle.entity.KittyPartyAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KittyPartyAuditLogRepository extends JpaRepository<KittyPartyAuditLog, Long> {

    List<KittyPartyAuditLog> findByCycleIdOrderByIdAsc(Long cycleId);

    Optional<KittyPartyAuditLog> findTopByCycleIdOrderByIdDesc(Long cycleId);

    List<KittyPartyAuditLog> findByCircleIdOrderByIdDesc(Long circleId);
}
