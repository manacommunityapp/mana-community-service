package com.manacommunity.api.privacy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDataExportDto {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String flatNo;
    private String block;
    private String tower;
    private String occupancyStatus;
    private LocalDateTime accountCreatedAt;

    private UserPrivacySettingsDto privacySettings;
    private List<FamilyMemberExportItem> familyMembers;
    private List<VisitorPassExportItem> visitorPasses;
    private List<MarketplaceOrderExportItem> marketplaceOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FamilyMemberExportItem {
        private Long id;
        private String name;
        private String relation;
        private Integer age;
        private String gender;
        private String phone;
        private String email;
        private String bloodGroup;
        private String gothram;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VisitorPassExportItem {
        private Long id;
        private String passCode;
        private String visitorName;
        private String visitorPhone;
        private String vehicleNumber;
        private String purpose;
        private String status;
        private LocalDateTime expectedAt;
        private LocalDateTime checkedInAt;
        private LocalDateTime checkedOutAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketplaceOrderExportItem {
        private Long id;
        private String orderNumber;
        private String role; // BUYER or SELLER
        private String status;
        private java.math.BigDecimal totalAmount;
        private LocalDateTime createdAt;
    }
}
