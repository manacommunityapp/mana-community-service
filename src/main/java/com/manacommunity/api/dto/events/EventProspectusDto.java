package com.manacommunity.api.dto.events;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventProspectusDto {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private String bankName;
    private String accountName;
    private String accountNumber;
    private String ifscCode;
    private String upiId;
    private String qrCodeUrl;
    private List<String> helplines;
    private List<PaymentContactPersonDto> contacts;
    private List<CorporatePackageDto> packages;
    private List<DonationSchemeDto> donations;
    private LocalDateTime updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentContactPersonDto {
        private String id;
        private String name;
        private String role;
        private String flatNo;
        private String phone;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorporatePackageDto {
        private String id;
        private String title;
        private String amount;
        private String badge;
        private List<String> features;
        private String color;
        private String totalSlots;
        private String claimedSlots;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonationSchemeDto {
        private String id;
        private String title;
        private String amount;
        private String desc;
        private String badge;
        private String category;
    }
}
