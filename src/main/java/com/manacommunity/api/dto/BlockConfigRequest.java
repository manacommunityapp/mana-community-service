package com.manacommunity.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Payload for admin create / update of a block config.
 */
@Data
public class BlockConfigRequest {

    @NotBlank
    private String blockName;       // A, B, C, D ...

    @Min(1) @Max(50)
    private int totalFloors = 10;

    @Min(1) @Max(100)
    private int flatsPerFloor;
}