package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.entity.HrReportConfig;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.repository.HrEmployeeRepository;
import com.manacommunity.api.hrm.repository.HrReportConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrReportService {

    private final HrReportConfigRepository reportConfigRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrReportConfig saveReportConfig(Long orgId, String name, String reportType, String filtersJson, String columnsJson, Long userId) {
        HrReportConfig config = HrReportConfig.builder()
                .organizationId(orgId)
                .name(name)
                .reportType(reportType)
                .filtersJson(filtersJson)
                .columnsJson(columnsJson)
                .createdBy(userId)
                .build();
        return reportConfigRepository.save(config);
    }

    public List<HrReportConfig> getReportConfigs(Long orgId) {
        return reportConfigRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    @Transactional
    public void deleteReportConfig(Long orgId, Long configId) {
        HrReportConfig config = reportConfigRepository.findById(configId)
                .filter(c -> c.getOrganizationId().equals(orgId))
                .orElseThrow(() -> new IllegalArgumentException("Report config not found"));
        reportConfigRepository.delete(config);
    }

    public byte[] generateCsvReport(Long orgId, String reportType) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        if ("HEADCOUNT".equalsIgnoreCase(reportType) || "CUSTOM".equalsIgnoreCase(reportType)) {
            writer.println("Employee Code,First Name,Last Name,Work Email,Department,Designation,Employment Type,Status,Joining Date");
            List<HrEmployee> employees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
            for (HrEmployee e : employees) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        safe(e.getEmployeeCode()),
                        safe(e.getFirstName()),
                        safe(e.getLastName()),
                        safe(e.getWorkEmail()),
                        e.getDepartment() != null ? safe(e.getDepartment().getName()) : "",
                        e.getDesignation() != null ? safe(e.getDesignation().getName()) : "",
                        safe(e.getEmploymentType() != null ? e.getEmploymentType().name() : ""),
                        safe(e.getEmploymentStatus() != null ? e.getEmploymentStatus().name() : ""),
                        e.getJoiningDate() != null ? e.getJoiningDate().toString() : "");
            }
        } else if ("ATTRITION".equalsIgnoreCase(reportType)) {
            writer.println("Employee Code,First Name,Last Name,Department,Exit Date");
            List<HrEmployee> exited = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.INACTIVE);
            for (HrEmployee e : exited) {
                writer.printf("%s,%s,%s,%s,%s%n",
                        safe(e.getEmployeeCode()),
                        safe(e.getFirstName()),
                        safe(e.getLastName()),
                        e.getDepartment() != null ? safe(e.getDepartment().getName()) : "",
                        e.getExitDate() != null ? e.getExitDate().toString() : "");
            }
        } else {
            writer.println("Report type not supported: " + reportType);
        }

        writer.flush();
        return baos.toByteArray();
    }

    private String safe(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
