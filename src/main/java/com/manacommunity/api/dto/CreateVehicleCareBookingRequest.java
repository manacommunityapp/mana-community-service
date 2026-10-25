package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVehicleCareBookingRequest {
    @NotNull
    private Long vehicleId;

    @NotNull
    private Long serviceId;

    @NotNull
    private Long slotId;

    private List<Long> addonIds;

    private String locationNotes;

    private String idempotencyKey;
}
