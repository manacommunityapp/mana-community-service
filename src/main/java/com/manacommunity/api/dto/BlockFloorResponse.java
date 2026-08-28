package com.manacommunity.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents one floor inside a block: the floor number and the list of
 * flat numbers that belong to it (computed in-memory).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlockFloorResponse {

    /** Human-readable floor label, e.g. "Floor 1". */
    private int floor;

    /**
     * Flat number strings for this floor, e.g. ["101","102",...,"111"].
     * For floors 1-9:  [floor*100+1 .. floor*100+N]
     * For floor 10:    [1001 .. 1000+N]
     */
    private List<String> flats;
}
