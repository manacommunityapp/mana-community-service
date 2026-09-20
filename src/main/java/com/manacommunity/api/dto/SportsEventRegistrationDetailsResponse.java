package com.manacommunity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsEventRegistrationDetailsResponse {
    private SportsEventResponse event;
    private List<SportsEventResponse.CategoryRef> categories;
    private List<SportsEventResponse> siblingCategories;
}

