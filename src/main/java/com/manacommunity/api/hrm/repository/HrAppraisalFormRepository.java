package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrAppraisalForm;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface HrAppraisalFormRepository extends JpaRepository<HrAppraisalForm, Long> {
    Optional<HrAppraisalForm> findByEmployeeIdAndAppraisalCycleId(Long employeeId, Long cycleId);
    List<HrAppraisalForm> findByAppraisalCycleId(Long cycleId);
    List<HrAppraisalForm> findByReviewerEmployeeIdAndStatus(Long reviewerId, String status);
}
