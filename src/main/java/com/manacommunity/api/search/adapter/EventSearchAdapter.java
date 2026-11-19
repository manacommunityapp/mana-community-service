package com.manacommunity.api.search.adapter;

import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventSearchAdapter implements SearchIndexAdapter {

    private final EventCommunityRepository eventRepository;

    @Override
    public SearchModule getModule() { return SearchModule.EVENT; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return eventRepository.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream()
                .map(e -> new SearchableItem(
                        communityId,
                        SearchModule.EVENT,
                        String.valueOf(e.getId()),
                        e.getTitle(),
                        e.getVenue() != null ? e.getVenue() : e.getLocation(),
                        e.getDescription(),
                        e.getOrganizerName() != null ? List.of(e.getOrganizerName()) : List.of(),
                        e.getImageUrl(),
                        "EVENT",
                        String.valueOf(e.getId()),
                        SearchVisibility.COMMUNITY,
                        null,
                        Map.of("location", e.getLocation() != null ? e.getLocation() : "",
                               "startDate", e.getStartDate() != null ? e.getStartDate().toString() : "")
                ))
                .toList();
    }
}
