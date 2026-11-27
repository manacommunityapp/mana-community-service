package com.manacommunity.api.delivery.dto;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressBatchDispatchRequest {

    @NotNull(message = "partner is required")
    private DeliveryPartner partner;

    @NotNull(message = "communityId is required")
    private Long communityId;

    @NotBlank(message = "riderName is required")
    private String riderName;

    @NotBlank(message = "riderPhone is required")
    private String riderPhone;

    private String vehicleNumber;
    private String riderPhotoUrl;
    private Integer etaMinutes;

    @NotEmpty(message = "packages list must not be empty")
    @Valid
    private List<BatchPackageItem> packages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchPackageItem {
        @NotBlank(message = "orderId is required")
        private String orderId;

        @NotBlank(message = "flatNumber is required")
        private String flatNumber;

        private String tower;
        private String residentName;
        private String residentPhone;
        private String itemsSummary;
    }
}
