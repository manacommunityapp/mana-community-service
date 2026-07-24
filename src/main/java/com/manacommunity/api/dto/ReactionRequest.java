package com.manacommunity.api.dto;

import com.manacommunity.api.model.ReactionType;
import jakarta.validation.constraints.NotNull;

public record ReactionRequest(
    @NotNull ReactionType reactionType
) {}
