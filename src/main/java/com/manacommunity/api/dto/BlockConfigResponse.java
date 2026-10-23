package com.manacommunity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full block configuration returned to the frontend dropdown.
 * Contains the block name, capacity metadata, and a pre-computed
 * floor -> flat-list hierarchy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockConfigResponse {

    /** Block label: A, B, C, D ... */
    private String blockName;

    /** Number of floors in this block. */
    private int totalFloors;

    /** Flats per floor (11 for A/B/D, 12 for C). */
    private int flatsPerFloor;

    /** Total flats = totalFloors * flatsPerFloor. */
    private int totalFlats;

    /**
     * Pre-computed floor-level breakdown.
     * Ordered from floor 1 to totalFloors.
     */
    private List<BlockFloorResponse> floors;
}