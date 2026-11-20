package com.manacommunity.api.search.adapter;

import com.manacommunity.api.marketplace.entity.MarketListing;
import com.manacommunity.api.marketplace.repository.MarketListingRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketplaceSearchAdapter implements SearchIndexAdapter {

    private final MarketListingRepository listingRepository;

    @Override
    public SearchModule getModule() {
        return SearchModule.MARKETPLACE_LISTING;
    }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        if (communityId == null) {
            return List.of();
        }
        return listingRepository.findByCommunityIdAndStatus(
                communityId,
                MarketListing.ListingStatus.ACTIVE,
                Pageable.ofSize(200)
        )
        .getContent()
        .stream()
        .map(item -> new SearchableItem(
                communityId,
                SearchModule.MARKETPLACE_LISTING,
                String.valueOf(item.getId()),
                item.getTitle(),
                item.getCategory() != null ? item.getCategory() : "Marketplace",
                item.getDescription(),
                item.getCategory() != null ? List.of(item.getCategory()) : List.of(),
                null,
                "MARKETPLACE_LISTING",
                String.valueOf(item.getId()),
                SearchVisibility.COMMUNITY,
                item.getSeller() != null ? item.getSeller().getId() : null,
                Map.of(
                        "price", item.getPrice() != null ? item.getPrice().toString() : "0",
                        "priceUnit", item.getPriceUnit() != null ? item.getPriceUnit() : "INR",
                        "category", item.getCategory() != null ? item.getCategory() : ""
                )
        ))
        .toList();
    }
}
