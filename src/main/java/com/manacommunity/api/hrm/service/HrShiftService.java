package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrShiftService {

    private final HrShiftRepository shiftRepository;
    private final HrEmployeeShiftRepository employeeShiftRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrShift createShift(HrOrganization org, CreateShiftRequest request) {
        HrShift shift = HrShift.builder()
                .organization(org)
                .shiftName(request.getShiftName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .breakMinutes(request.getBreakMinutes() != null ? request.getBreakMinutes() : 0)
                .build();
        return shiftRepository.save(shift);
    }

    @Transactional(readOnly = true)
    public List<HrShift> listShifts(Long orgId) {
        return shiftRepository.findByOrganizationIdAndIsActiveTrue(orgId);
    }

    @Transactional
    public HrEmployeeShift assignShift(Long orgId, AssignShiftRequest request) {
        HrEmployee employee = employeeRepository.findByIdAndOrganizationId(request.getEmployeeId(), orgId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        HrShift shift = shiftRepository.findById(request.getShiftId())
                .orElseThrow(() -> new IllegalArgumentException("Shift not found"));

        if (!shift.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Shift does not belong to this organization");
        }

        HrEmployeeShift assignment = HrEmployeeShift.builder()
                .organizationId(orgId)
                .employee(employee)
                .shift(shift)
                .shiftDate(request.getShiftDate())
                .build();
        return employeeShiftRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<HrEmployeeShift> getEmployeeShifts(Long employeeId, LocalDate from, LocalDate to) {
        return employeeShiftRepository.findByEmployeeIdAndShiftDateBetween(employeeId, from, to);
    }

    @Transactional(readOnly = true)
    public Page<HrEmployeeShift> getOrgShiftsForDate(Long orgId, LocalDate date, Pageable pageable) {
        return employeeShiftRepository.findByOrganizationIdAndShiftDate(orgId, date, pageable);
    }
}
