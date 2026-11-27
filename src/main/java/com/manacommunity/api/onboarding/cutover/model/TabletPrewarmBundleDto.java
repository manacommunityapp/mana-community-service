package com.manacommunity.api.onboarding.cutover.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TabletPrewarmBundleDto {

    private String bundleId;
    private Long communityId;
    private LocalDateTime generatedAt;
    private int schoolBusesCount;
    private int domesticStaffCount;
    private int residentWhitelistCount;
    private int vehicleRfidsCount;
    private long compressedBundleSizeBytes;
    private String bundleChecksumSha256;
    private String downloadUrl;

    private List<SchoolBusPass> schoolBuses;
    private List<DomesticStaffFastPass> domesticStaff;
    private List<VehicleRfidTag> vehicleRfids;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchoolBusPass {
        private String registrationNumber;
        private String schoolName;
        private String driverName;
        private String driverPhone;
        private List<String> studentFlatNumbers;
        private boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomesticStaffFastPass {
        private String staffId;
        private String staffName;
        private String category; // COOK, MAID, DRIVER, NANNY
        private String rfidCardNumber;
        private String qrCodeHash;
        private List<String> authorizedFlats;
        private boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleRfidTag {
        private String epcHex;
        private String plateNumber;
        private String towerFlat;
        private boolean active;
    }
}
