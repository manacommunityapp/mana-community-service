package com.manacommunity.api.hrm.repository;

import com.manacommunity.api.hrm.entity.HrEmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface HrEmployeeDocumentRepository extends JpaRepository<HrEmployeeDocument, Long> {

    List<HrEmployeeDocument> findByEmployeeId(Long employeeId);

    List<HrEmployeeDocument> findByOrganizationIdAndExpiryDateBefore(Long organizationId, LocalDate date);

    java.util.Optional<HrEmployeeDocument> findByIdAndOrganizationId(Long id, Long organizationId);
}
