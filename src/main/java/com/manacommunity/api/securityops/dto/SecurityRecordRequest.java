package com.manacommunity.api.securityops.dto;

public record SecurityRecordRequest(String name, String phone, String vehicleNumber, String validFrom, String validUntil, String timeWindow, String purpose, String location, String status, String notes, String otp) {}
