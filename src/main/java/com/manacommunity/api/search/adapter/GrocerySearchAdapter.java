package com.manacommunity.api.search.adapter;

import com.manacommunity.api.repository.GroceryItemRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GrocerySearchAdapter implements SearchIndexAdapter {

    private final GroceryItemRepository groceryItemRepository;

    @Override
    public SearchModule getModule() { return SearchModule.GROCERY_PRODUCT; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return groceryItemRepository.findByCommunityIdOrderByNameAsc(communityId)
                .stream()
                .filter(g -> Boolean.TRUE.equals(g.getAvailable()))
                .map(g -> new SearchableItem(
                        communityId,
                        SearchModule.GROCERY_PRODUCT,
                        String.valueOf(g.getId()),
                        g.getName(),
                        g.getVendorName(),
                        g.getCategory(),
                        List.of("grocery", "food", g.getCategory() != null ? g.getCategory() : ""),
                        g.getImageUrl(),
                        "GROCERY",
                        String.valueOf(g.getId()),
                        SearchVisibility.COMMUNITY,
                        null,
                        Map.of("price", g.getPricePerUnit().toPlainString(),
                               "unit", g.getUnit() != null ? g.getUnit() : "")
                ))
                .toList();
    }
}
