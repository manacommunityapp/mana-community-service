package com.manacommunity.api.sports.dto;

import lombok.Data;
import java.util.List;

@Data
public class SportsMultiUnregisterRequest {
    private List<Long> registrationIds;
    private String reason;
}
