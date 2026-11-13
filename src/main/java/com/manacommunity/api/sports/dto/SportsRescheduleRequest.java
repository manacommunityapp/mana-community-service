package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

import jakarta.validation.constraints.NotBlank;

public record SportsRescheduleRequest(
    @NotBlank String scheduledAt,
    String venue,
    String court
) {
    public SportsRescheduleRequest(String scheduledAt, String venue) {
        this(scheduledAt, venue, null);
    }
}
