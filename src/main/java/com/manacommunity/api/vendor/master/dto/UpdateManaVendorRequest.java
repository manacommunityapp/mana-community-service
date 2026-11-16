package com.manacommunity.api.vendor.master.dto;

import lombok.Data;

@Data
public class UpdateManaVendorRequest {
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
    private String status;
}
