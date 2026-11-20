package com.manacommunity.api.search.adapter;

import com.manacommunity.api.document.entity.DocAccessLevel;
import com.manacommunity.api.document.entity.Document;
import com.manacommunity.api.document.entity.DocumentStatus;
import com.manacommunity.api.document.repository.DocumentRepository;
import com.manacommunity.api.search.SearchModule;
import com.manacommunity.api.search.SearchVisibility;
import com.manacommunity.api.search.SearchableItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentSearchAdapter implements SearchIndexAdapter {

    private final DocumentRepository documentRepository;

    @Override
    public SearchModule getModule() {
        return SearchModule.DOCUMENT;
    }

    @Override
    public List<SearchableItem> fetchAll(Long communityId) {
        if (communityId == null) {
            return List.of();
        }
        return documentRepository.findByCommunityIdAndStatusNot(communityId, DocumentStatus.DELETED)
                .stream()
                .map(doc -> {
                    SearchVisibility visibility;
                    if (doc.getAccessLevel() == DocAccessLevel.PUBLIC) {
                        visibility = SearchVisibility.PUBLIC;
                    } else if (doc.getAccessLevel() == DocAccessLevel.COMMUNITY) {
                        visibility = SearchVisibility.COMMUNITY;
                    } else {
                        visibility = SearchVisibility.MEMBERS_ONLY;
                    }
                    boolean isCommunityWide = visibility != SearchVisibility.MEMBERS_ONLY;
                    String categoryStr = doc.getCategory() != null ? doc.getCategory().name() : "GENERAL";

                    return new SearchableItem(
                            communityId,
                            SearchModule.DOCUMENT,
                            String.valueOf(doc.getId()),
                            doc.getTitle(),
                            categoryStr.replace("_", " "),
                            doc.getDescription(),
                            List.of(categoryStr),
                            null,
                            "DOCUMENT",
                            String.valueOf(doc.getId()),
                            visibility,
                            isCommunityWide ? null : doc.getOwnerUserId(),
                            Map.of(
                                    "category", categoryStr,
                                    "accessLevel", doc.getAccessLevel() != null ? doc.getAccessLevel().name() : "",
                                    "entityType", doc.getEntityType() != null ? doc.getEntityType() : ""
                            )
                    );
                })
                .toList();
    }
}
