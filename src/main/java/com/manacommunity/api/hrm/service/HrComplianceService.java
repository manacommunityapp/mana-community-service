package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrComplianceAlert;
import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.repository.HrComplianceAlertRepository;
import com.manacommunity.api.hrm.repository.HrEmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrComplianceService {

    private final HrComplianceAlertRepository alertRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public int generateComplianceAlerts(Long orgId) {
        List<HrEmployee> activeEmployees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        LocalDate today = LocalDate.now();
        int generated = 0;

        for (HrEmployee emp : activeEmployees) {
            // Probation end: confirmationDate within next 30 days
            if (emp.getConfirmationDate() != null) {
                LocalDate probEnd = emp.getConfirmationDate();
                if (!probEnd.isBefore(today) && !probEnd.isAfter(today.plusDays(30))) {
                    if (!alertRepository.existsByOrganizationIdAndEmployeeIdAndAlertTypeAndDueDate(orgId, emp.getId(), "PROBATION_ENDING", probEnd)) {
                        String name = emp.getFirstName() + " " + (emp.getLastName() != null ? emp.getLastName() : "");
                        alertRepository.save(HrComplianceAlert.builder()
                                .organizationId(orgId)
                                .employee(emp)
                                .alertType("PROBATION_ENDING")
                                .title("Probation ending for " + name.trim())
                                .description("Employee " + name.trim() + " probation period ends on " + probEnd)
                                .dueDate(probEnd)
                                .severity(probEnd.isBefore(today.plusDays(7)) ? "HIGH" : "MEDIUM")
                                .build());
                        generated++;
                    }
                }
            }

            // Contract expiry: exitDate within next 60 days (for contract employees)
            if (emp.getExitDate() != null) {
                LocalDate exitDate = emp.getExitDate();
                if (!exitDate.isBefore(today) && !exitDate.isAfter(today.plusDays(60))) {
                    if (!alertRepository.existsByOrganizationIdAndEmployeeIdAndAlertTypeAndDueDate(orgId, emp.getId(), "CONTRACT_EXPIRY", exitDate)) {
                        String name = emp.getFirstName() + " " + (emp.getLastName() != null ? emp.getLastName() : "");
                        alertRepository.save(HrComplianceAlert.builder()
                                .organizationId(orgId)
                                .employee(emp)
                                .alertType("CONTRACT_EXPIRY")
                                .title("Contract expiry for " + name.trim())
                                .description("Employee " + name.trim() + " contract expires on " + exitDate)
                                .dueDate(exitDate)
                                .severity(exitDate.isBefore(today.plusDays(14)) ? "HIGH" : "MEDIUM")
                                .build());
                        generated++;
                    }
                }
            }
        }

        return generated;
    }

    public List<HrComplianceAlert> getOpenAlerts(Long orgId) {
        return alertRepository.findByOrganizationIdAndStatusOrderByDueDateAsc(orgId, "OPEN");
    }

    public List<HrComplianceAlert> getAllAlerts(Long orgId) {
        return alertRepository.findByOrganizationIdOrderByDueDateAsc(orgId);
    }

    @Transactional
    public HrComplianceAlert acknowledgeAlert(Long orgId, Long alertId, Long userId) {
        HrComplianceAlert alert = alertRepository.findById(alertId)
                .filter(a -> a.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setStatus("ACKNOWLEDGED");
        alert.setAcknowledgedBy(userId);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    @Transactional
    public HrComplianceAlert resolveAlert(Long orgId, Long alertId) {
        HrComplianceAlert alert = alertRepository.findById(alertId)
                .filter(a -> a.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setStatus("RESOLVED");
        return alertRepository.save(alert);
    }

    @Transactional
    public HrComplianceAlert dismissAlert(Long orgId, Long alertId) {
        HrComplianceAlert alert = alertRepository.findById(alertId)
                .filter(a -> a.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setStatus("DISMISSED");
        return alertRepository.save(alert);
    }
}
