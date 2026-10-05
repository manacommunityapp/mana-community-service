package com.manacommunity.api.service;

import com.manacommunity.api.dto.PantryItemRequest;
import com.manacommunity.api.dto.PantryItemResponse;

import java.util.List;

public interface PantryService {

    List<PantryItemResponse> getUserPantryItems(Long userId);

    PantryItemResponse getPantryItem(Long id, Long userId);

    PantryItemResponse createPantryItem(PantryItemRequest request, Long userId);

    PantryItemResponse updatePantryItem(Long id, PantryItemRequest request, Long userId);

    void deletePantryItem(Long id, Long userId);

    List<PantryItemResponse> getExpiringSoonItems(Long userId);
}
