package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrExpenseService {

    private final HrExpenseClaimRepository claimRepository;
    private final HrExpenseItemRepository itemRepository;
    private final HrTravelRequestRepository travelRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrExpenseClaim createClaim(Long orgId, Long employeeId, HrExpenseClaim claim, List<HrExpenseItem> items) {
        HrEmployee emp = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        claim.setOrganizationId(orgId);
        claim.setEmployee(emp);
        HrExpenseClaim saved = claimRepository.save(claim);
        double total = 0;
        for (HrExpenseItem item : items) {
            item.setExpenseClaim(saved);
            itemRepository.save(item);
            total += item.getAmount();
        }
        saved.setTotalAmount(total);
        return claimRepository.save(saved);
    }

    @Transactional
    public HrExpenseClaim submitClaim(Long orgId, Long claimId) {
        HrExpenseClaim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new EntityNotFoundException("Expense claim not found"));
        if (!claim.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        if (!"DRAFT".equals(claim.getStatus())) throw new IllegalStateException("Only DRAFT claims can be submitted");
        claim.setStatus("SUBMITTED");
        return claimRepository.save(claim);
    }

    @Transactional
    public HrExpenseClaim approveClaim(Long orgId, Long claimId, Double approvedAmount, Long approvedBy) {
        HrExpenseClaim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new EntityNotFoundException("Claim not found"));
        if (!claim.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        claim.setStatus("APPROVED");
        claim.setApprovedAmount(approvedAmount != null ? approvedAmount : claim.getTotalAmount());
        claim.setApprovedBy(approvedBy);
        claim.setApprovedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    @Transactional
    public HrExpenseClaim rejectClaim(Long orgId, Long claimId, String reason, Long rejectedBy) {
        HrExpenseClaim claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new EntityNotFoundException("Claim not found"));
        if (!claim.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        claim.setStatus("REJECTED");
        claim.setRejectionReason(reason);
        claim.setApprovedBy(rejectedBy);
        claim.setApprovedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    public Page<HrExpenseClaim> getMyClaims(Long employeeId, Pageable pageable) {
        return claimRepository.findByEmployeeId(employeeId, pageable);
    }

    public Page<HrExpenseClaim> getPendingClaims(Long orgId, Pageable pageable) {
        return claimRepository.findByOrganizationIdAndStatus(orgId, "SUBMITTED", pageable);
    }

    public List<HrExpenseItem> getItems(Long claimId) {
        return itemRepository.findByExpenseClaimId(claimId);
    }

    @Transactional
    public HrTravelRequest createTravelRequest(Long orgId, Long employeeId, HrTravelRequest request) {
        HrEmployee emp = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        request.setOrganizationId(orgId);
        request.setEmployee(emp);
        return travelRepository.save(request);
    }

    @Transactional
    public HrTravelRequest approveTravelRequest(Long orgId, Long requestId, Long approvedBy) {
        HrTravelRequest req = travelRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Travel request not found"));
        if (!req.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        req.setStatus("APPROVED");
        req.setApprovedBy(approvedBy);
        req.setApprovedAt(LocalDateTime.now());
        return travelRepository.save(req);
    }

    @Transactional
    public HrTravelRequest rejectTravelRequest(Long orgId, Long requestId, String reason, Long rejectedBy) {
        HrTravelRequest req = travelRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Travel request not found"));
        if (!req.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        req.setStatus("REJECTED");
        req.setRejectionReason(reason);
        req.setApprovedBy(rejectedBy);
        req.setApprovedAt(LocalDateTime.now());
        return travelRepository.save(req);
    }

    public Page<HrTravelRequest> getMyTravelRequests(Long employeeId, Pageable pageable) {
        return travelRepository.findByEmployeeId(employeeId, pageable);
    }

    public Page<HrTravelRequest> getPendingTravelRequests(Long orgId, Pageable pageable) {
        return travelRepository.findByOrganizationIdAndStatus(orgId, "PENDING", pageable);
    }
}
