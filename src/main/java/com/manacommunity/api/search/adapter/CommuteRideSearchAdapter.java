package com.manacommunity.api.search.adapter;

import com.manacommunity.api.model.CommuteRideStatus;
import com.manacommunity.api.repository.CommuteRideRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CommuteRideSearchAdapter implements SearchIndexAdapter {

    private final CommuteRideRepository commuteRideRepository;

    @Override
    public SearchModule getModule() { return SearchModule.RIDE; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return commuteRideRepository
                .findUpcomingRides(communityId,
                        List.of(CommuteRideStatus.ACTIVE),
                        LocalDateTime.now(),
                        null,
                        PageRequest.of(0, 500))
                .getContent()
                .stream()
                .map(r -> new SearchableItem(
                        communityId,
                        SearchModule.RIDE,
                        String.valueOf(r.getId()),
                        r.getFromLocation() + " → " + r.getToLocation(),
                        r.getDriver() != null ? r.getDriver().getFullName() : "",
                        r.getRideType() != null ? r.getRideType().name() : "",
                        List.of("ride", "commute", "carpool",
                                r.getVehicleType() != null ? r.getVehicleType() : ""),
                        null,
                        "RIDE",
                        String.valueOf(r.getId()),
                        SearchVisibility.COMMUNITY,
                        r.getDriver() != null ? r.getDriver().getId() : null,
                        Map.of("from", r.getFromLocation(),
                               "to", r.getToLocation(),
                               "seats", String.valueOf(r.getAvailableSeats()))
                ))
                .toList();
    }
}
