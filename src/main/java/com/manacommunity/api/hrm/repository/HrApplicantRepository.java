package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrApplicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
public interface HrApplicantRepository extends JpaRepository<HrApplicant, Long> {
    Page<HrApplicant> findByOrganizationId(Long orgId, Pageable pageable);
    Page<HrApplicant> findByJobPostingId(Long jobPostingId, Pageable pageable);
    Page<HrApplicant> findByJobPostingIdAndStatus(Long jobPostingId, String status, Pageable pageable);
}
