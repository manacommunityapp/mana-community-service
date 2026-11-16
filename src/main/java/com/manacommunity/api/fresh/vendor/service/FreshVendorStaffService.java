package com.manacommunity.api.fresh.vendor.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fresh.vendor.dto.InviteStaffRequest;
import com.manacommunity.api.fresh.vendor.dto.VendorStaffDto;
import com.manacommunity.api.fresh.vendor.model.FreshVendorStaff;
import com.manacommunity.api.fresh.vendor.model.VendorStaffRole;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorStaffRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshVendorStaffService {

    private final FreshVendorStaffRepository staffRepository;
    private final VmsVendorRepository vendorRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public VendorStaffDto assignOrUpdateStaff(InviteStaffRequest request, Long invitingUserId) {
        VmsVendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("VmsVendor", String.valueOf(request.getVendorId())));

        AppUser targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", String.valueOf(request.getUserId())));

        AppUser invitingUser = invitingUserId != null ? userRepository.findById(invitingUserId).orElse(null) : null;

        FreshVendorStaff staff = staffRepository.findByVendorIdAndUserId(vendor.getId(), targetUser.getId())
                .orElseGet(() -> FreshVendorStaff.builder()
                        .vendor(vendor)
                        .user(targetUser)
                        .build());

        staff.setVendorRole(request.getVendorRole());
        staff.setIsActive(true);
        if (invitingUser != null) {
            staff.setInvitedByUser(invitingUser);
        }

        FreshVendorStaff saved = staffRepository.save(staff);
        log.info("Staff member {} assigned role {} for vendor {}", targetUser.getEmail(), saved.getVendorRole(), vendor.getBusinessName());
        return VendorStaffDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<VendorStaffDto> getStaffByVendor(Long vendorId) {
        return staffRepository.findByVendorId(vendorId).stream()
                .map(VendorStaffDto::fromEntity)
                .toList();
    }

    @Transactional
    public void deactivateStaff(Long vendorId, Long staffUserId) {
        FreshVendorStaff staff = staffRepository.findByVendorIdAndUserId(vendorId, staffUserId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshVendorStaff", "vendor " + vendorId + " user " + staffUserId));

        staff.setIsActive(false);
        staffRepository.save(staff);
        log.info("Staff member {} deactivated for vendor {}", staffUserId, vendorId);
    }

    @Transactional(readOnly = true)
    public boolean hasVendorRole(Long vendorId, Long userId, VendorStaffRole role) {
        return staffRepository.existsByVendorIdAndUserIdAndVendorRoleAndIsActiveTrue(vendorId, userId, role);
    }

    @Transactional(readOnly = true)
    public boolean isVendorStaff(Long vendorId, Long userId) {
        return staffRepository.existsByVendorIdAndUserIdAndIsActiveTrue(vendorId, userId);
    }
}
