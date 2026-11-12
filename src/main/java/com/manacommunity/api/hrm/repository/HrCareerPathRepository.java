package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrCareerPath;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface HrCareerPathRepository extends JpaRepository<HrCareerPath, Long> {
    Optional<HrCareerPath> findByEmployeeIdAndStatus(Long employeeId, String status);
    List<HrCareerPath> findByOrganizationId(Long orgId);
}
