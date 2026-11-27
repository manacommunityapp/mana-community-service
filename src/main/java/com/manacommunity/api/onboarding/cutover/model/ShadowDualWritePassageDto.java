package com.manacommunity.api.onboarding.cutover.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShadowDualWritePassageDto {

    private String passageId;
    private Long communityId;
    private String gateIdentifier;
    private String rfidTagOrPlate;
    private String passageType; // SCHOOL_BUS, DAILY_HELP, RESIDENT_VEHICLE, DELIVERY
    private LocalDateTime legacyTimestamp;
    private LocalDateTime ingestedAt;
    private boolean reconciledWithLegacy;
}
