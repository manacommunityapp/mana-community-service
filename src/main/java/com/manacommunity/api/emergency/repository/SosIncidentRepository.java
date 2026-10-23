package com.manacommunity.api.emergency.repository;

import com.manacommunity.api.emergency.entity.SosIncident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SosIncidentRepository extends JpaRepository<SosIncident, Long> {

    List<SosIncident> findByCommunityIdAndStatusInOrderByTriggeredAtDesc(
            Long communityId, List<SosIncident.IncidentStatus> statuses);

    Page<SosIncident> findByCommunityIdOrderByTriggeredAtDesc(Long communityId, Pageable pageable);

    List<SosIncident> findByResidentIdOrderByTriggeredAtDesc(Long residentId);

    @Query("SELECT COUNT(i) FROM SosIncident i WHERE i.community.id = :communityId AND i.status NOT IN ('RESOLVED', 'FALSE_ALARM')")
    long countActiveIncidents(@Param("communityId") Long communityId);
}