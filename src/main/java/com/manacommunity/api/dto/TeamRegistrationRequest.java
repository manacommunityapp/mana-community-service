package com.manacommunity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TeamRegistrationRequest {
    @NotNull
    Long eventId;
    @NotNull
    Long categoryId;
    @NotBlank
    @Size(max = 100)
    String teamName;
    String logoEmoji;
    @NotNull
    @Size(min = 1, max = 20)
    List<Long> memberUserIds;
    Long captainUserId;
}
