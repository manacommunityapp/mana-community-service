package com.manacommunity.api.safety.dto;

import com.manacommunity.api.safety.model.GuardShift;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShiftStatusRequest {

    @NotNull
    private GuardShift.ShiftStatus status;
}
