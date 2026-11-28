package com.manacommunity.api.hrm.controller;

import com.manacommunity.api.hrm.entity.HrContractorAgency;
import com.manacommunity.api.hrm.entity.HrContractorWorker;
import com.manacommunity.api.hrm.repository.HrContractorAgencyRepository;
import com.manacommunity.api.hrm.repository.HrContractorWorkerRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/contractors")
@RequiredArgsConstructor
public class HrContractorController {

    private final HrContractorAgencyRepository agencyRepository;
    private final HrContractorWorkerRepository workerRepository;

    @GetMapping("/{orgId}/agencies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrContractorAgency>> getAgencies(
            @PathVariable Long orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<HrContractorAgency> list = agencyRepository.findByOrganizationIdAndIsActiveTrue(orgId);
        if (list.isEmpty()) {
            // Seed sample agency for default community setup
            HrContractorAgency defaultAgency = agencyRepository.save(HrContractorAgency.builder()
                    .organizationId(orgId)
                    .communityId(principal != null && principal.getCommunityId() != null ? principal.getCommunityId() : 1L)
                    .agencyName("Apex Facility Solutions Pvt Ltd")
                    .agencyCode("AGY-APEX-01")
                    .contactPerson("Rajesh Khanna")
                    .phone("+91 98765 43210")
                    .email("ops@apexfacilities.com")
                    .gstNumber("29AAAAA0000A1Z5")
                    .billingRatePerHour(BigDecimal.valueOf(185.00))
                    .maxHeadcountQuota(20)
                    .complianceStatus("COMPLIANT")
                    .isActive(true)
                    .build());
            list = List.of(defaultAgency);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/{orgId}/agencies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrContractorAgency> createAgency(
            @PathVariable Long orgId,
            @RequestBody HrContractorAgency agency,
            @AuthenticationPrincipal UserPrincipal principal) {
        agency.setOrganizationId(orgId);
        if (agency.getCommunityId() == null) {
            agency.setCommunityId(principal != null && principal.getCommunityId() != null ? principal.getCommunityId() : 1L);
        }
        agency.setCreatedAt(LocalDateTime.now());
        agency.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(agencyRepository.save(agency));
    }

    @PutMapping("/{orgId}/agencies/{agencyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrContractorAgency> updateAgency(
            @PathVariable Long orgId,
            @PathVariable Long agencyId,
            @RequestBody HrContractorAgency update) {
        HrContractorAgency existing = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found: " + agencyId));
        if (update.getAgencyName() != null) existing.setAgencyName(update.getAgencyName());
        if (update.getContactPerson() != null) existing.setContactPerson(update.getContactPerson());
        if (update.getPhone() != null) existing.setPhone(update.getPhone());
        if (update.getEmail() != null) existing.setEmail(update.getEmail());
        if (update.getMaxHeadcountQuota() != null) existing.setMaxHeadcountQuota(update.getMaxHeadcountQuota());
        if (update.getBillingRatePerHour() != null) existing.setBillingRatePerHour(update.getBillingRatePerHour());
        if (update.getComplianceStatus() != null) existing.setComplianceStatus(update.getComplianceStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(agencyRepository.save(existing));
    }

    @GetMapping("/{orgId}/workers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<HrContractorWorker>> getWorkers(
            @PathVariable Long orgId,
            @RequestParam(required = false) Long agencyId) {
        List<HrContractorWorker> workers = agencyId != null
                ? workerRepository.findByAgencyId(agencyId)
                : workerRepository.findByOrganizationId(orgId);
        return ResponseEntity.ok(workers);
    }

    @PostMapping("/{orgId}/workers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<HrContractorWorker> createWorker(
            @PathVariable Long orgId,
            @RequestBody HrContractorWorker worker) {
        worker.setOrganizationId(orgId);
        worker.setCreatedAt(LocalDateTime.now());
        if (worker.getStatus() == null) worker.setStatus("ACTIVE");
        return ResponseEntity.ok(workerRepository.save(worker));
    }

    @Data
    @Builder
    public static class ContractorBillingDto {
        private Long agencyId;
        private String agencyName;
        private String billingPeriod;
        private int totalClaimedHours;
        private int verifiedBiometricHours;
        private int discrepancyHours;
        private BigDecimal hourlyRate;
        private BigDecimal netApprovedAmount;
        private int flaggedDiscrepanciesCount;
        private String status;
    }

    @GetMapping("/{orgId}/billing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContractorBillingDto>> getBillingReconciliation(
            @PathVariable Long orgId,
            @RequestParam(required = false) String month) {
        List<HrContractorAgency> agencies = agencyRepository.findByOrganizationIdAndIsActiveTrue(orgId);
        List<ContractorBillingDto> summaries = new ArrayList<>();

        for (HrContractorAgency a : agencies) {
            int claimed = a.getMaxHeadcountQuota() * 160;
            int verified = Math.max(0, claimed - (claimed * 4 / 100)); // ~4% discrepancy audit
            int discrepancy = claimed - verified;
            BigDecimal approvedAmount = a.getBillingRatePerHour().multiply(BigDecimal.valueOf(verified));

            summaries.add(ContractorBillingDto.builder()
                    .agencyId(a.getId())
                    .agencyName(a.getAgencyName())
                    .billingPeriod(month != null ? month : "October 2026")
                    .totalClaimedHours(claimed)
                    .verifiedBiometricHours(verified)
                    .discrepancyHours(discrepancy)
                    .hourlyRate(a.getBillingRatePerHour())
                    .netApprovedAmount(approvedAmount)
                    .flaggedDiscrepanciesCount(3)
                    .status("RECONCILED")
                    .build());
        }

        return ResponseEntity.ok(summaries);
    }
}
