package com.manacommunity.api.fresh.vendor.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.fresh.vendor.dto.ApplyCapabilityRequest;
import com.manacommunity.api.fresh.vendor.dto.ApproveCapabilityRequest;
import com.manacommunity.api.fresh.vendor.dto.VendorCapabilityDto;
import com.manacommunity.api.fresh.vendor.model.CapabilityStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapability;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorCapabilityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreshVendorCapabilityService {

    private final FreshVendorCapabilityRepository capabilityRepository;
    private final VmsVendorRepository vendorRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public VendorCapabilityDto applyForCapability(ApplyCapabilityRequest request) {
        VmsVendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException("VmsVendor", String.valueOf(request.getVendorId())));

        FreshVendorCapability capability = capabilityRepository
                .findByVendorIdAndCapabilityType(vendor.getId(), request.getCapabilityType())
                .orElseGet(() -> FreshVendorCapability.builder()
                        .vendor(vendor)
                        .capabilityType(request.getCapabilityType())
                        .build());

        capability.setStatus(CapabilityStatus.PENDING_APPROVAL);
        capability.setFssaiLicenseNumber(request.getFssaiLicenseNumber());
        capability.setFssaiValidUntil(request.getFssaiValidUntil());
        capability.setFacilityAddress(request.getFacilityAddress());
        capability.setColdStorageAvailable(Boolean.TRUE.equals(request.getColdStorageAvailable()));
        capability.setDailyCapacityUnits(request.getDailyCapacityUnits() != null ? request.getDailyCapacityUnits() : 0);
        capability.setOperatingDays(request.getOperatingDays() != null ? request.getOperatingDays() : "MON,TUE,WED,THU,FRI,SAT,SUN");
        capability.setRejectionReason(null);

        FreshVendorCapability saved = capabilityRepository.save(capability);
        log.info("Vendor {} applied for capability {}", vendor.getBusinessName(), saved.getCapabilityType());
        return VendorCapabilityDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<VendorCapabilityDto> getCapabilitiesByVendor(Long vendorId) {
        return capabilityRepository.findByVendorId(vendorId).stream()
                .map(VendorCapabilityDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VendorCapabilityDto> getPendingCapabilities() {
        return capabilityRepository.findByStatus(CapabilityStatus.PENDING_APPROVAL).stream()
                .map(VendorCapabilityDto::fromEntity)
                .toList();
    }

    @Transactional
    public VendorCapabilityDto reviewCapability(Long capabilityId, ApproveCapabilityRequest request, Long adminUserId) {
        FreshVendorCapability capability = capabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshVendorCapability", String.valueOf(capabilityId)));

        AppUser adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", String.valueOf(adminUserId)));

        if (Boolean.TRUE.equals(request.getApproved())) {
            capability.setStatus(CapabilityStatus.APPROVED);
            capability.setApprovedByUser(adminUser);
            capability.setApprovedAt(LocalDateTime.now());
            capability.setRejectionReason(null);
            log.info("Capability {} for vendor {} APPROVED by admin {}",
                    capability.getCapabilityType(), capability.getVendor().getBusinessName(), adminUser.getEmail());
        } else {
            capability.setStatus(CapabilityStatus.REJECTED);
            capability.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason() : "Requirements not met");
            capability.setApprovedByUser(adminUser);
            capability.setApprovedAt(LocalDateTime.now());
            log.info("Capability {} for vendor {} REJECTED by admin {}: {}",
                    capability.getCapabilityType(), capability.getVendor().getBusinessName(), adminUser.getEmail(), capability.getRejectionReason());
        }

        return VendorCapabilityDto.fromEntity(capabilityRepository.save(capability));
    }

    @Transactional
    public VendorCapabilityDto suspendCapability(Long capabilityId, String reason, Long adminUserId) {
        FreshVendorCapability capability = capabilityRepository.findById(capabilityId)
                .orElseThrow(() -> new ResourceNotFoundException("FreshVendorCapability", String.valueOf(capabilityId)));

        AppUser adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", String.valueOf(adminUserId)));

        capability.setStatus(CapabilityStatus.SUSPENDED);
        capability.setRejectionReason(reason);
        capability.setApprovedByUser(adminUser);
        capability.setApprovedAt(LocalDateTime.now());

        log.warn("Capability {} for vendor {} SUSPENDED by admin {}: {}",
                capability.getCapabilityType(), capability.getVendor().getBusinessName(), adminUser.getEmail(), reason);
        return VendorCapabilityDto.fromEntity(capabilityRepository.save(capability));
    }

    @Transactional(readOnly = true)
    public boolean hasApprovedCapability(Long vendorId, FreshVendorCapabilityType capabilityType) {
        return capabilityRepository.existsByVendorIdAndCapabilityTypeAndStatus(
                vendorId, capabilityType, CapabilityStatus.APPROVED);
    }
}
