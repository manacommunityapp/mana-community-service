package com.manacommunity.api.securityops.dto;

import com.manacommunity.api.securityops.entity.CommunitySecurityRecord;
import java.time.LocalDateTime;

public record SecurityRecordResponse(Long id, String recordType, String name, String phone, String vehicleNumber, String validFrom, String validUntil, String timeWindow, String purpose, String location, String status, String notes, LocalDateTime createdAt, String generatedOtp) {
    public static SecurityRecordResponse from(CommunitySecurityRecord r, String generatedOtp) { return new SecurityRecordResponse(r.getId(), r.getRecordType(), r.getName(), r.getPhone(), r.getVehicleNumber(), r.getValidFrom(), r.getValidUntil(), r.getTimeWindow(), r.getPurpose(), r.getLocation(), r.getStatus(), r.getNotes(), r.getCreatedAt(), generatedOtp); }
}
