package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.model.SecurityIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, Long> {

    List<SecurityIncident> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<SecurityIncident> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);
}
