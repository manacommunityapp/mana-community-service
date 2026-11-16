package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrJobPosting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HrJobPostingRepository extends JpaRepository<HrJobPosting, Long> {
    Page<HrJobPosting> findByOrganizationId(Long orgId, Pageable pageable);
    Page<HrJobPosting> findByOrganizationIdAndStatus(Long orgId, String status, Pageable pageable);
}
