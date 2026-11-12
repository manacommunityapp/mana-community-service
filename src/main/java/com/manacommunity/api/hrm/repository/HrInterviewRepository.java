package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrInterview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrInterviewRepository extends JpaRepository<HrInterview, Long> {
    List<HrInterview> findByApplicantId(Long applicantId);
    List<HrInterview> findByOrganizationIdAndInterviewerEmployeeId(Long orgId, Long interviewerEmployeeId);
}
