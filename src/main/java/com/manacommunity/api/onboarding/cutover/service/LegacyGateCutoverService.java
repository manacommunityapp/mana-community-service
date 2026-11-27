package com.manacommunity.api.onboarding.cutover.service;

import com.manacommunity.api.onboarding.cutover.model.*;

import java.time.LocalDateTime;
import java.util.List;

public interface LegacyGateCutoverService {

    TabletPrewarmBundleDto generateTabletPrewarmBundle(
            Long communityId,
            List<TabletPrewarmBundleDto.SchoolBusPass> schoolBuses,
            List<DomesticStaffFastPassDto> domesticStaff,
            List<TabletPrewarmBundleDto.VehicleRfidTag> vehicleRfids,
            int totalResidents);

    void registerTabletDevice(Long communityId, String tabletDeviceId, String gateIdentifier);

    void recordTabletPrewarmAck(Long communityId, String tabletDeviceId, String verifiedChecksumSha256);

    void ingestShadowDualWriteEvent(ShadowDualWritePassageDto event);

    CutoverReadinessAuditDto auditCutoverReadiness(Long communityId, LocalDateTime targetCutoverTime);

    CutoverExecutionResultDto executeCutover(Long communityId);

    // Supporting DTO for domestic staff pass
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class DomesticStaffFastPassDto {
        private String staffId;
        private String staffName;
        private String category;
        private String rfidCardNumber;
        private String qrCodeHash;
        private List<String> authorizedFlats;
        private boolean active;
    }
}
