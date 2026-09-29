package com.manacommunity.api.repository;

import com.manacommunity.api.model.ContentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentReportRepository extends JpaRepository<ContentReport, Long> {
    Page<ContentReport> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, String status, Pageable pageable);
    Page<ContentReport> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);
    boolean existsByContentTypeAndContentIdAndReportedById(String contentType, Long contentId, Long reportedById);
    long countByCommunityIdAndStatus(Long communityId, String status);
}
