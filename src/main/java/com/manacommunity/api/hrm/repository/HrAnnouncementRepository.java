package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HrAnnouncementRepository extends JpaRepository<HrAnnouncement, Long> {
    @Query("SELECT a FROM HrAnnouncement a WHERE a.organizationId = :orgId AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<HrAnnouncement> findActiveByOrganizationId(@Param("orgId") Long orgId);
}
