package com.manacommunity.api.search.adapter;

import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import com.manacommunity.api.trip.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TripSearchAdapter implements SearchIndexAdapter {

    private final TripRepository tripRepository;

    @Override
    public SearchModule getModule() { return SearchModule.TRIP; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return tripRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(t -> communityId.equals(t.getCommunityId()))
                .map(t -> new SearchableItem(
                        communityId,
                        SearchModule.TRIP,
                        t.getId(),
                        t.getTitle(),
                        t.getDestination(),
                        t.getDeparturePoint() + " → " + t.getDestination(),
                        List.of("trip", "travel", t.getCategory() != null ? t.getCategory() : "",
                                t.getTransport() != null ? t.getTransport() : ""),
                        null,
                        "TRIP",
                        t.getId(),
                        SearchVisibility.COMMUNITY,
                        t.getOrganizerUserId(),
                        Map.of("departureDate", t.getDepartureDate() != null ? t.getDepartureDate() : "",
                               "price", t.getPricePerPerson().toPlainString())
                ))
                .toList();
    }
}
