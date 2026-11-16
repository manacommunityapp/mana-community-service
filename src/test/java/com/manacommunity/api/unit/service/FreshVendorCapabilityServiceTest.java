package com.manacommunity.api.unit.service;

import com.manacommunity.api.fresh.vendor.dto.ApplyCapabilityRequest;
import com.manacommunity.api.fresh.vendor.dto.ApproveCapabilityRequest;
import com.manacommunity.api.fresh.vendor.dto.VendorCapabilityDto;
import com.manacommunity.api.fresh.vendor.model.CapabilityStatus;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapability;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import com.manacommunity.api.fresh.vendor.repository.FreshVendorCapabilityRepository;
import com.manacommunity.api.fresh.vendor.service.FreshVendorCapabilityService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import com.manacommunity.api.vendor.repository.VmsVendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshVendorCapabilityServiceTest {

    @Mock
    private FreshVendorCapabilityRepository capabilityRepository;
    @Mock
    private VmsVendorRepository vendorRepository;
    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private FreshVendorCapabilityService capabilityService;

    private VmsVendor testVendor;
    private AppUser testAdmin;

    @BeforeEach
    void setUp() {
        testVendor = VmsVendor.builder()
                .id(10L)
                .businessName("Deccan Organic Farms")
                .build();

        testAdmin = new AppUser();
        testAdmin.setId(99L);
        testAdmin.setEmail("admin@manacommunity.com");
    }

    @Test
    void testApplyForCapability_CreatesPendingCapability() {
        when(vendorRepository.findById(10L)).thenReturn(Optional.of(testVendor));
        when(capabilityRepository.findByVendorIdAndCapabilityType(10L, FreshVendorCapabilityType.BATTER_MANUFACTURER))
                .thenReturn(Optional.empty());
        when(capabilityRepository.save(any(FreshVendorCapability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApplyCapabilityRequest request = ApplyCapabilityRequest.builder()
                .vendorId(10L)
                .capabilityType(FreshVendorCapabilityType.BATTER_MANUFACTURER)
                .fssaiLicenseNumber("FSSAI-11521018000452")
                .fssaiValidUntil(LocalDate.of(2027, 12, 31))
                .facilityAddress("Plot 12, IDA Nacharam, Hyderabad")
                .coldStorageAvailable(true)
                .dailyCapacityUnits(500)
                .operatingDays("MON,TUE,WED,THU,FRI,SAT")
                .build();

        VendorCapabilityDto result = capabilityService.applyForCapability(request);

        assertNotNull(result);
        assertEquals(FreshVendorCapabilityType.BATTER_MANUFACTURER, result.getCapabilityType());
        assertEquals(CapabilityStatus.PENDING_APPROVAL, result.getStatus());
        assertEquals("FSSAI-11521018000452", result.getFssaiLicenseNumber());
        assertTrue(result.getColdStorageAvailable());
        assertEquals(500, result.getDailyCapacityUnits());
    }

    @Test
    void testReviewCapability_AdminApprove_SetsApprovedStatus() {
        FreshVendorCapability capability = FreshVendorCapability.builder()
                .id(1L)
                .vendor(testVendor)
                .capabilityType(FreshVendorCapabilityType.FRESH_CUT_PROCESSOR)
                .status(CapabilityStatus.PENDING_APPROVAL)
                .build();

        when(capabilityRepository.findById(1L)).thenReturn(Optional.of(capability));
        when(userRepository.findById(99L)).thenReturn(Optional.of(testAdmin));
        when(capabilityRepository.save(any(FreshVendorCapability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApproveCapabilityRequest request = ApproveCapabilityRequest.builder()
                .approved(true)
                .build();

        VendorCapabilityDto result = capabilityService.reviewCapability(1L, request, 99L);

        assertEquals(CapabilityStatus.APPROVED, result.getStatus());
        assertEquals(99L, result.getApprovedByUserId());
        assertNotNull(result.getApprovedAt());
    }

    @Test
    void testReviewCapability_AdminReject_SetsRejectedStatusWithReason() {
        FreshVendorCapability capability = FreshVendorCapability.builder()
                .id(2L)
                .vendor(testVendor)
                .capabilityType(FreshVendorCapabilityType.CHICKEN_SUPPLIER)
                .status(CapabilityStatus.PENDING_APPROVAL)
                .build();

        when(capabilityRepository.findById(2L)).thenReturn(Optional.of(capability));
        when(userRepository.findById(99L)).thenReturn(Optional.of(testAdmin));
        when(capabilityRepository.save(any(FreshVendorCapability.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ApproveCapabilityRequest request = ApproveCapabilityRequest.builder()
                .approved(false)
                .rejectionReason("Inadequate cold-chain temperature validation probe certificates")
                .build();

        VendorCapabilityDto result = capabilityService.reviewCapability(2L, request, 99L);

        assertEquals(CapabilityStatus.REJECTED, result.getStatus());
        assertEquals("Inadequate cold-chain temperature validation probe certificates", result.getRejectionReason());
    }
}