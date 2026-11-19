package com.manacommunity.api.search.adapter;

import com.manacommunity.api.academy.repository.AcademyProgramRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AcademyCourseSearchAdapter implements SearchIndexAdapter {

    private final AcademyProgramRepository academyProgramRepository;

    @Override
    public SearchModule getModule() { return SearchModule.COURSE; }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        return academyProgramRepository.findByCommunityIdAndStatusNotOrderByCreatedAtDesc(communityId, "COMPLETED")
                .stream()
                .map(p -> new SearchableItem(
                        communityId,
                        SearchModule.COURSE,
                        String.valueOf(p.getId()),
                        p.getTitle(),
                        p.getInstructorName(),
                        p.getDescription(),
                        List.of("course", "class", "academy",
                                p.getCategory() != null ? p.getCategory().toLowerCase() : ""),
                        null,
                        "COURSE",
                        String.valueOf(p.getId()),
                        SearchVisibility.COMMUNITY,
                        p.getInstructorId(),
                        Map.of("category", p.getCategory() != null ? p.getCategory() : "",
                               "fee", p.getFee().toPlainString(),
                               "status", p.getStatus() != null ? p.getStatus() : "")
                ))
                .toList();
    }
}
