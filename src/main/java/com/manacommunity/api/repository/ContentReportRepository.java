package com.manacommunity.api.repository;

import com.manacommunity.api.model.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {

    Page<ContentReport> findByCommunityIdAndStatus(Long communityId, String status, Pageable pageable);

    Page<ContentReport> findByCommunityId(Long communityId, Pageable pageable);

    long countByCommunityIdAndStatus(Long communityId, String status);

    boolean existsByTargetTypeAndTargetIdAndReporterId(String targetType, Long targetId, Long reporterId);
}
