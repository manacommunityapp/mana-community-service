package com.manacommunity.api.search.adapter;

import com.manacommunity.api.homeservices.repository.HomeServiceProviderRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HomeServiceSearchAdapter implements SearchIndexAdapter {

    private final HomeServiceProviderRepository homeServiceProviderRepository;

    @Override
    public SearchModule getModule() { return SearchModule.HOME_SERVICE; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return homeServiceProviderRepository.findByCommunityId(communityId)
                .stream()
                .filter(h -> Boolean.TRUE.equals(h.getIsAvailable()))
                .map(h -> {
                    String categoryName = h.getCategory() != null ? h.getCategory().getName() : "";
                    return new SearchableItem(
                            communityId,
                            SearchModule.HOME_SERVICE,
                            String.valueOf(h.getId()),
                            h.getName(),
                            categoryName,
                            null,
                            List.of("home service", categoryName, "vendor"),
                            null,
                            "HOME_SERVICE",
                            String.valueOf(h.getId()),
                            SearchVisibility.COMMUNITY,
                            h.getUserId(),
                            Map.of("rating", h.getRating().toPlainString(),
                                   "verified", String.valueOf(Boolean.TRUE.equals(h.getIsVerified())))
                    );
                })
                .toList();
    }
}
