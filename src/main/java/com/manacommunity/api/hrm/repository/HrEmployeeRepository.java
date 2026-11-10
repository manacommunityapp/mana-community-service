package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HrEmployeeRepository extends JpaRepository<HrEmployee, Long> {

    Page<HrEmployee> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<HrEmployee> findByOrganizationIdAndEmploymentStatus(Long organizationId, EmploymentStatus status, Pageable pageable);

    List<HrEmployee> findByManaUserId(Long manaUserId);

    Optional<HrEmployee> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<HrEmployee> findByOrganizationIdAndManaUserId(Long organizationId, Long manaUserId);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndEmploymentStatus(Long organizationId, EmploymentStatus status);

    boolean existsByOrganizationIdAndEmployeeCode(Long organizationId, String employeeCode);

    boolean existsByOrganizationIdAndManaUserId(Long organizationId, Long manaUserId);

    long countByOrganizationIdAndJoiningDateBetween(Long organizationId, java.time.LocalDate from, java.time.LocalDate to);

    List<HrEmployee> findByOrganizationIdAndEmploymentStatus(Long organizationId, EmploymentStatus status);

    @Query("SELECT e FROM HrEmployee e WHERE e.organization.id = :orgId AND e.reportingManagerId IS NULL AND e.employmentStatus = 'ACTIVE'")
    List<HrEmployee> findRootEmployeesByOrg(@Param("orgId") Long orgId);

    @Query("SELECT e FROM HrEmployee e WHERE e.reportingManagerId = :managerId AND e.employmentStatus = 'ACTIVE'")
    List<HrEmployee> findDirectReports(@Param("managerId") Long managerId);
}
