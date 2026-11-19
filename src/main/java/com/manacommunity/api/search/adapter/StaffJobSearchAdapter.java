package com.manacommunity.api.search.adapter;

import com.manacommunity.api.homeservices.repository.StaffJobPostRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StaffJobSearchAdapter implements SearchIndexAdapter {

    private final StaffJobPostRepository staffJobPostRepository;

    @Override
    public SearchModule getModule() { return SearchModule.JOB; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return staffJobPostRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .filter(j -> "OPEN".equals(j.getStatus() != null ? j.getStatus().name() : ""))
                .map(j -> new SearchableItem(
                        communityId,
                        SearchModule.JOB,
                        String.valueOf(j.getId()),
                        j.getTitle(),
                        j.getRole() != null ? j.getRole().name() : "",
                        j.getDescription(),
                        List.of("job", "hiring", "domestic staff",
                                j.getRole() != null ? j.getRole().name().toLowerCase() : ""),
                        null,
                        "JOB",
                        String.valueOf(j.getId()),
                        SearchVisibility.COMMUNITY,
                        null,
                        Map.of("salary", j.getSalaryRange() != null ? j.getSalaryRange() : "",
                               "shift", j.getShiftPreference() != null ? j.getShiftPreference() : "")
                ))
                .toList();
    }
}
