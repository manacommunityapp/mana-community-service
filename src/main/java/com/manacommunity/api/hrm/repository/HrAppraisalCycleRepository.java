package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrAppraisalCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrAppraisalCycleRepository extends JpaRepository<HrAppraisalCycle, Long> {
    List<HrAppraisalCycle> findByOrganizationId(Long orgId);
    List<HrAppraisalCycle> findByOrganizationIdAndStatus(Long orgId, String status);
}
