package com.manacommunity.api.gate.repository;

import com.manacommunity.api.gate.model.SecurityAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, Long> {

    Page<SecurityAlert> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<SecurityAlert> findByCommunityIdAndAlertStatusOrderByCreatedAtDesc(
            Long communityId, SecurityAlert.AlertStatus alertStatus, Pageable pageable);

    List<SecurityAlert> findByCommunityIdAndAlertStatusOrderByCreatedAtDesc(
            Long communityId, SecurityAlert.AlertStatus alertStatus);

    long countByCommunityIdAndAlertStatus(Long communityId, SecurityAlert.AlertStatus alertStatus);
}
