package com.manacommunity.api.privacy;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.privacy.dto.UserDataExportDto;
import com.manacommunity.api.privacy.dto.UserPrivacySettingsDto;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.repository.FamilyMemberRepository;
import com.manacommunity.api.visitor.repository.VisitorPassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDataExportService {

    private final AppUserRepository userRepo;
    private final FamilyMemberRepository familyMemberRepo;
    private final VisitorPassRepository visitorPassRepo;
    private final MarketOrderRepository marketOrderRepo;
    private final UserPrivacySettingsService privacySettingsService;
    private final PrivacyAuditService privacyAuditService;

    @Transactional(readOnly = true)
    public UserDataExportDto exportUserData(Long userId) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        UserPrivacySettingsDto privacySettings = privacySettingsService.getSettings(userId);

        // Family members
        List<UserDataExportDto.FamilyMemberExportItem> familyList = familyMemberRepo
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream().map(f -> UserDataExportDto.FamilyMemberExportItem.builder()
                        .id(f.getId())
                        .name(f.getName())
                        .relation(f.getRelation())
                        .age(f.getAge())
                        .gender(f.getGender())
                        .phone(f.getPhone())
                        .email(f.getEmail())
                        .bloodGroup(f.getBloodGroup())
                        .gothram(f.getGothram())
                        .build())
                .toList();

        // Visitor passes (as resident)
        List<UserDataExportDto.VisitorPassExportItem> visitorList = visitorPassRepo
                .findByResidentIdOrderByCreatedAtDesc(userId)
                .stream().map(v -> UserDataExportDto.VisitorPassExportItem.builder()
                        .id(v.getId())
                        .passCode(v.getPassCode())
                        .visitorName(v.getVisitorName())
                        .visitorPhone(v.getVisitorPhone())
                        .vehicleNumber(v.getVehicleNumber())
                        .purpose(v.getPurpose())
                        .status(v.getStatus() != null ? v.getStatus().name() : null)
                        .expectedAt(v.getExpectedAt())
                        .checkedInAt(v.getCheckedInAt())
                        .checkedOutAt(v.getCheckedOutAt())
                        .build())
                .toList();

        // Marketplace orders (buyer + seller roles)
        List<UserDataExportDto.MarketplaceOrderExportItem> orderList = new ArrayList<>();

        marketOrderRepo.findByBuyerId(userId, Pageable.unpaged())
                .forEach(o -> orderList.add(UserDataExportDto.MarketplaceOrderExportItem.builder()
                        .id(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .role("BUYER")
                        .status(o.getStatus() != null ? o.getStatus().name() : null)
                        .totalAmount(o.getTotalAmount())
                        .createdAt(o.getCreatedAt())
                        .build()));

        marketOrderRepo.findBySellerId(userId, Pageable.unpaged())
                .forEach(o -> orderList.add(UserDataExportDto.MarketplaceOrderExportItem.builder()
                        .id(o.getId())
                        .orderNumber(o.getOrderNumber())
                        .role("SELLER")
                        .status(o.getStatus() != null ? o.getStatus().name() : null)
                        .totalAmount(o.getTotalAmount())
                        .createdAt(o.getCreatedAt())
                        .build()));

        // Audit the data export
        privacyAuditService.record(
                AuditAction.EXPORT_PERSONAL_DATA.name(),
                "USER",
                String.valueOf(userId),
                user.getCommunity() != null ? user.getCommunity().getId() : null,
                "User requested personal data export"
        );

        return UserDataExportDto.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .flatNo(user.getFlatNo())
                .block(user.getBlock())
                .tower(user.getTower())
                .occupancyStatus(user.getOccupancyStatus())
                .accountCreatedAt(user.getCreatedAt())
                .privacySettings(privacySettings)
                .familyMembers(familyList)
                .visitorPasses(visitorList)
                .marketplaceOrders(orderList)
                .build();
    }
}
