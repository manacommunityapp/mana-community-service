package com.manacommunity.api.guard.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.guard.dto.*;
import com.manacommunity.api.guard.entity.GuardProfile;
import com.manacommunity.api.guard.entity.GuardShift;
import com.manacommunity.api.guard.repository.GuardProfileRepository;
import com.manacommunity.api.guard.repository.GuardShiftRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuardService {

    private final GuardProfileRepository profileRepository;
    private final GuardShiftRepository shiftRepository;
    private final AppUserRepository userRepository;

    // ── Guard Profiles ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GuardProfileResponse> getGuards(Long communityId) {
        return profileRepository.findByCommunityIdOrderByFullNameAsc(communityId)
                .stream().map(this::toProfileResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GuardProfileResponse> getActiveGuards(Long communityId) {
        return profileRepository.findByCommunityIdAndStatusOrderByFullNameAsc(communityId, GuardProfile.GuardStatus.ACTIVE)
                .stream().map(this::toProfileResponse).toList();
    }

    @Transactional
    public GuardProfileResponse createGuard(AppUser caller, GuardProfileRequest req) {
        Long communityId = requireCommunity(caller);

        if (req.employeeId() != null && !req.employeeId().isBlank()) {
            profileRepository.findByCommunityIdAndEmployeeId(communityId, req.employeeId())
                    .ifPresent(g -> { throw new InvalidInputException("Employee ID " + req.employeeId() + " already exists."); });
        }

        AppUser linkedUser = null;
        if (req.userId() != null) {
            linkedUser = userRepository.findById(req.userId()).orElse(null);
        }

        GuardProfile profile = GuardProfile.builder()
                .community(caller.getCommunity())
                .fullName(req.fullName())
                .phone(req.phone())
                .employeeId(req.employeeId())
                .assignedGate(req.assignedGate())
                .user(linkedUser)
                .notes(req.notes())
                .build();

        return toProfileResponse(profileRepository.save(profile));
    }

    @Transactional
    public GuardProfileResponse updateGuard(Long guardId, GuardProfileRequest req) {
        GuardProfile profile = profileRepository.findById(guardId)
                .orElseThrow(() -> new ResourceNotFoundException("GuardProfile", guardId));

        profile.setFullName(req.fullName());
        profile.setPhone(req.phone());
        profile.setEmployeeId(req.employeeId());
        profile.setAssignedGate(req.assignedGate());
        profile.setNotes(req.notes());

        if (req.userId() != null) {
            profile.setUser(userRepository.findById(req.userId()).orElse(null));
        } else {
            profile.setUser(null);
        }

        return toProfileResponse(profileRepository.save(profile));
    }

    @Transactional
    public GuardProfileResponse updateGuardStatus(Long guardId, String status) {
        GuardProfile profile = profileRepository.findById(guardId)
                .orElseThrow(() -> new ResourceNotFoundException("GuardProfile", guardId));

        try {
            profile.setStatus(GuardProfile.GuardStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new InvalidInputException("Invalid status: " + status);
        }

        return toProfileResponse(profileRepository.save(profile));
    }

    @Transactional
    public void deleteGuard(Long guardId) {
        if (!profileRepository.existsById(guardId)) {
            throw new ResourceNotFoundException("GuardProfile", guardId);
        }
        profileRepository.deleteById(guardId);
    }

    // ── Guard Shifts ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GuardShiftResponse> getShiftsByDate(Long communityId, LocalDate date) {
        return shiftRepository.findByCommunityIdAndShiftDateOrderByStartTimeAsc(communityId, date)
                .stream().map(this::toShiftResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GuardShiftResponse> getShiftsByRange(Long communityId, LocalDate from, LocalDate to) {
        return shiftRepository.findByCommunityIdAndShiftDateBetweenOrderByShiftDateAscStartTimeAsc(communityId, from, to)
                .stream().map(this::toShiftResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GuardShiftResponse> getGuardShifts(Long guardId) {
        return shiftRepository.findByGuardIdOrderByShiftDateDescStartTimeDesc(guardId)
                .stream().map(this::toShiftResponse).toList();
    }

    @Transactional
    public GuardShiftResponse createShift(AppUser caller, GuardShiftRequest req) {
        Long communityId = requireCommunity(caller);

        GuardProfile guard = profileRepository.findById(req.guardId())
                .orElseThrow(() -> new ResourceNotFoundException("GuardProfile", req.guardId()));

        GuardShift shift = GuardShift.builder()
                .community(caller.getCommunity())
                .guard(guard)
                .shiftDate(req.shiftDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .gate(req.gate())
                .notes(req.notes())
                .build();

        return toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public GuardShiftResponse updateShift(Long shiftId, GuardShiftRequest req) {
        GuardShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("GuardShift", shiftId));

        GuardProfile guard = profileRepository.findById(req.guardId())
                .orElseThrow(() -> new ResourceNotFoundException("GuardProfile", req.guardId()));

        shift.setGuard(guard);
        shift.setShiftDate(req.shiftDate());
        shift.setStartTime(req.startTime());
        shift.setEndTime(req.endTime());
        shift.setGate(req.gate());
        shift.setNotes(req.notes());

        return toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public GuardShiftResponse checkInShift(Long shiftId) {
        GuardShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("GuardShift", shiftId));

        shift.setStatus(GuardShift.ShiftStatus.IN_PROGRESS);
        shift.setCheckInTime(LocalDateTime.now());
        return toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public GuardShiftResponse checkOutShift(Long shiftId) {
        GuardShift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("GuardShift", shiftId));

        shift.setStatus(GuardShift.ShiftStatus.COMPLETED);
        shift.setCheckOutTime(LocalDateTime.now());
        return toShiftResponse(shiftRepository.save(shift));
    }

    @Transactional
    public void deleteShift(Long shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new ResourceNotFoundException("GuardShift", shiftId);
        }
        shiftRepository.deleteById(shiftId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private Long requireCommunity(AppUser caller) {
        if (caller.getCommunity() == null) {
            throw new InvalidInputException("User is not associated with any community.");
        }
        return caller.getCommunity().getId();
    }

    private GuardProfileResponse toProfileResponse(GuardProfile p) {
        return GuardProfileResponse.builder()
                .id(p.getId())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .employeeId(p.getEmployeeId())
                .assignedGate(p.getAssignedGate())
                .userId(p.getUser() != null ? p.getUser().getId() : null)
                .userName(p.getUser() != null ? p.getUser().getFullName() : null)
                .status(p.getStatus().name())
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private GuardShiftResponse toShiftResponse(GuardShift s) {
        return GuardShiftResponse.builder()
                .id(s.getId())
                .guardId(s.getGuard().getId())
                .guardName(s.getGuard().getFullName())
                .shiftDate(s.getShiftDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .gate(s.getGate())
                .status(s.getStatus().name())
                .checkInTime(s.getCheckInTime())
                .checkOutTime(s.getCheckOutTime())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
