package com.manacommunity.api.grouporder.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class CreateGroupOrderRequest {

    @NotBlank @Size(max = 300)
    private String title;

    private String description;

    @NotBlank @Size(max = 50)
    private String category;

    @Size(max = 200)
    private String sourceVendor;

    @NotBlank @Size(max = 300)
    private String itemName;

    @DecimalMin("0.01")
    private BigDecimal unitPrice;

    @Size(max = 100)
    private String unitDescription;

    @Min(2)
    private int minQuantity = 5;

    private Integer maxQuantity;

    @NotNull
    private OffsetDateTime closesAt;

    private LocalDate deliveryDate;

    @Size(max = 200)
    private String deliveryPoint;

    @Size(max = 500)
    private String imageUrl;
}
