package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrShiftSwapService {

    private final HrShiftSwapRepository swapRepository;
    private final HrEmployeeRepository employeeRepository;
    private final HrEmployeeShiftRepository employeeShiftRepository;

    @Transactional
    public HrShiftSwapRequest requestSwap(Long orgId, Long requesterEmployeeId, Long targetEmployeeId,
                                           LocalDate requesterDate, LocalDate targetDate, String reason) {
        HrEmployee requester = employeeRepository.findById(requesterEmployeeId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Requester not found"));
        HrEmployee target = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Target employee not found"));

        employeeShiftRepository.findByEmployeeIdAndShiftDate(requesterEmployeeId, requesterDate)
                .orElseThrow(() -> new IllegalArgumentException("You don't have a shift on " + requesterDate));
        employeeShiftRepository.findByEmployeeIdAndShiftDate(targetEmployeeId, targetDate)
                .orElseThrow(() -> new IllegalArgumentException("Target employee doesn't have a shift on " + targetDate));

        return swapRepository.save(HrShiftSwapRequest.builder()
                .organizationId(orgId)
                .requesterEmployee(requester)
                .targetEmployee(target)
                .requesterShiftDate(requesterDate)
                .targetShiftDate(targetDate)
                .reason(reason)
                .build());
    }

    @Transactional
    public HrShiftSwapRequest colleagueRespond(Long swapId, boolean accepted, String rejectionReason) {
        HrShiftSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Swap request not found"));
        if (!"PENDING_COLLEAGUE".equals(swap.getStatus())) {
            throw new IllegalStateException("Swap is not awaiting colleague response");
        }
        swap.setColleagueRespondedAt(LocalDateTime.now());
        swap.setStatus(accepted ? "PENDING_MANAGER" : "REJECTED");
        if (!accepted) swap.setRejectionReason(rejectionReason);
        return swapRepository.save(swap);
    }

    @Transactional
    public HrShiftSwapRequest managerRespond(Long swapId, boolean approved, Long approverId, String reason) {
        HrShiftSwapRequest swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Swap request not found"));
        if (!"PENDING_MANAGER".equals(swap.getStatus())) {
            throw new IllegalStateException("Swap is not awaiting manager approval");
        }
        swap.setManagerRespondedAt(LocalDateTime.now());
        swap.setApprovedBy(approverId);
        swap.setStatus(approved ? "APPROVED" : "REJECTED");
        if (!approved) swap.setRejectionReason(reason);

        if (approved) {
            HrEmployeeShift requesterShift = employeeShiftRepository
                    .findByEmployeeIdAndShiftDate(swap.getRequesterEmployee().getId(), swap.getRequesterShiftDate())
                    .orElseThrow(() -> new IllegalStateException("Requester shift no longer exists"));
            HrEmployeeShift targetShift = employeeShiftRepository
                    .findByEmployeeIdAndShiftDate(swap.getTargetEmployee().getId(), swap.getTargetShiftDate())
                    .orElseThrow(() -> new IllegalStateException("Target shift no longer exists"));

            // Swap the employees on the shift assignments
            HrEmployee temp = requesterShift.getEmployee();
            requesterShift.setEmployee(targetShift.getEmployee());
            targetShift.setEmployee(temp);
            employeeShiftRepository.save(requesterShift);
            employeeShiftRepository.save(targetShift);
        }
        return swapRepository.save(swap);
    }

    @Transactional(readOnly = true)
    public List<HrShiftSwapRequest> getMySwapRequests(Long employeeId) {
        return swapRepository.findByRequesterEmployeeId(employeeId);
    }

    @Transactional(readOnly = true)
    public List<HrShiftSwapRequest> getIncomingSwapRequests(Long employeeId) {
        return swapRepository.findByTargetEmployeeIdAndStatus(employeeId, "PENDING_COLLEAGUE");
    }

    @Transactional(readOnly = true)
    public List<HrShiftSwapRequest> getPendingManagerApprovals(Long orgId) {
        return swapRepository.findByOrganizationIdAndStatus(orgId, "PENDING_MANAGER");
    }
}
