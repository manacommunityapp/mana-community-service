package com.manacommunity.api.safety.repository;

import com.manacommunity.api.safety.model.SecurityIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SecurityIncidentRepository extends JpaRepository<SecurityIncident, Long> {

    List<SecurityIncident> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<SecurityIncident> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status);

    @Query("SELECT COUNT(i) FROM SecurityIncident i WHERE i.communityId = :cid AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    long countOpenByCommunityId(@Param("cid") Long communityId);
}
