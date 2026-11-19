package com.manacommunity.api.search.adapter;

import com.manacommunity.api.events.entity.EventPoojaType;
import com.manacommunity.api.events.repository.PoojaTypeRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PoojaSearchAdapter implements SearchIndexAdapter {

    private final PoojaTypeRepository poojaTypeRepository;

    @Override
    public SearchModule getModule() { return SearchModule.POOJA; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return poojaTypeRepository.findByCommunityIdOrCommunityIdIsNullOrderByNameAsc(communityId)
                .stream()
                .map(p -> new SearchableItem(
                        communityId,
                        SearchModule.POOJA,
                        String.valueOf(p.getId()),
                        p.getName(),
                        null,
                        p.getDescription(),
                        List.of("pooja", "puja", "religious"),
                        null,
                        "POOJA",
                        String.valueOf(p.getId()),
                        SearchVisibility.COMMUNITY,
                        null,
                        Map.of()
                ))
                .toList();
    }
}
