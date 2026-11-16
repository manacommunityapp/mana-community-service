package com.manacommunity.api.vendor.master.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ManaVendorDto {
    private Long id;
    private Long communityId;
    private String name;
    private String contactPerson;
    private String email;
    private String phone;
    private String gstin;
    private String pan;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String bankDetails;
    private String categories;
    private Integer rating;
    private String sourceModule;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
