package com.manacommunity.api.food.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RestaurantRequest {
    @NotBlank
    private String name;
    private String description;
    private String cuisineTypes;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String phone;
    private String email;
    private String logoUrl;
    private String coverImageUrl;
    private String fssaiLicense;
    private String gstNumber;
    private Boolean deliveryEnabled;
    private Boolean takeawayEnabled;
    private Boolean dineInEnabled;
    private BigDecimal minOrderAmount;
    private List<OperatingHoursRequest> operatingHours;
}
