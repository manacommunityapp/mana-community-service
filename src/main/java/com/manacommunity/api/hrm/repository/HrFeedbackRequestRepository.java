package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrFeedbackRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrFeedbackRequestRepository extends JpaRepository<HrFeedbackRequest, Long> {
    List<HrFeedbackRequest> findBySubjectEmployeeId(Long employeeId);
    List<HrFeedbackRequest> findByOrganizationIdAndStatus(Long orgId, String status);
}
