package com.manacommunity.api.hrm.repository;
import com.manacommunity.api.hrm.entity.HrOkr;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface HrOkrRepository extends JpaRepository<HrOkr, Long> {
    List<HrOkr> findByOrganizationId(Long orgId);
    List<HrOkr> findByEmployeeId(Long employeeId);
    List<HrOkr> findByOrganizationIdAndYearAndQuarter(Long orgId, Integer year, String quarter);
}
