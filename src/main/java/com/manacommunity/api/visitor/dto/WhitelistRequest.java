package com.manacommunity.api.visitor.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class WhitelistRequest {
    @JsonAlias({"visitorName", "guestName"})
    private String name;

    @JsonAlias({"visitorPhone", "phoneNumber"})
    private String phone;

    private String category;
    private String vehicleNumber;
    private String notes;

    public void setVisitorName(String visitorName) {
        if (this.name == null || this.name.isBlank()) {
            this.name = visitorName;
        }
    }

    public void setVisitorPhone(String visitorPhone) {
        if (this.phone == null || this.phone.isBlank()) {
            this.phone = visitorPhone;
        }
    }
}
