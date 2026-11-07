package com.manacommunity.api.sports.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SportsStreamStartRequest {
    @Size(max = 300)
    private String title;
}
