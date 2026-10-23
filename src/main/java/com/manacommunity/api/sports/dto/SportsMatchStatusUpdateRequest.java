package com.manacommunity.api.sports.dto;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.dto.*;

/**
 * Request body for PUT /api/tournament/{configId}/matches/status.
 * Updates the status of every match belonging to the config —
 * used by the "Save as Draft" (DRAFT) and "Save & Publish" (PUBLISHED) buttons.
 */
public record SportsMatchStatusUpdateRequest(String status) {}