package com.manacommunity.api.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.search.adapter.SearchIndexAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchIndexService {

    private final SearchIndexRepository searchIndexRepository;
    private final List<SearchIndexAdapter> adapters;
    private final ObjectMapper objectMapper;

    @Transactional
    public void index(SearchableItem item) {
        SearchIndex existing = searchIndexRepository
                .findByCommunityIdAndModuleAndEntityId(item.communityId(), item.module(), item.entityId())
                .orElse(null);

        String tagsText = item.tags() != null
                ? item.tags().stream().filter(t -> t != null && !t.isBlank()).collect(Collectors.joining(","))
                : null;
        String metadataJson = toJson(item.metadata());

        if (existing != null) {
            existing.setTitle(item.title());
            existing.setSubtitle(item.subtitle());
            existing.setDescription(item.description());
            existing.setTags(tagsText);
            existing.setImageUrl(item.imageUrl());
            existing.setDeepLinkType(item.deepLinkType());
            existing.setDeepLinkId(item.deepLinkId());
            existing.setVisibility(item.visibility());
            existing.setOwnerUserId(item.ownerUserId());
            existing.setMetadata(metadataJson);
            existing.setActive(true);
            existing.setUpdatedAt(Instant.now());
            searchIndexRepository.save(existing);
        } else {
            SearchIndex si = SearchIndex.builder()
                    .communityId(item.communityId())
                    .module(item.module())
                    .entityId(item.entityId())
                    .title(item.title())
                    .subtitle(item.subtitle())
                    .description(item.description())
                    .tags(tagsText)
                    .imageUrl(item.imageUrl())
                    .deepLinkType(item.deepLinkType())
                    .deepLinkId(item.deepLinkId())
                    .visibility(item.visibility())
                    .ownerUserId(item.ownerUserId())
                    .metadata(metadataJson)
                    .build();
            searchIndexRepository.save(si);
        }
    }

    @Transactional
    public void remove(Long communityId, SearchModule module, String entityId) {
        searchIndexRepository.deleteByCommunityIdAndModuleAndEntityId(communityId, module, entityId);
    }

    @Transactional
    public void deactivate(Long communityId, SearchModule module, String entityId) {
        searchIndexRepository
                .findByCommunityIdAndModuleAndEntityId(communityId, module, entityId)
                .ifPresent(si -> {
                    si.setActive(false);
                    si.setUpdatedAt(Instant.now());
                    searchIndexRepository.save(si);
                });
    }

    @Transactional(readOnly = true)
    public SearchResponse search(Long communityId, String query, List<String> modules, int page, int size) {
        int offset = page * size;
        String modulesFilter = (modules == null || modules.isEmpty()) ? null : String.join(",", modules);

        List<Object[]> rows = searchIndexRepository.searchFts(communityId, query, modulesFilter, size, offset);
        List<SearchResultDto> results = rows.stream().map(this::mapRow).toList();

        List<Object[]> facetRows = searchIndexRepository.countFacets(communityId, query);
        Map<String, Long> facets = new HashMap<>();
        for (Object[] row : facetRows) {
            facets.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        long total = facets.values().stream().mapToLong(Long::longValue).sum();
        return new SearchResponse(results, facets, total, page, size);
    }

    @Transactional
    public int reindexCommunity(Long communityId) {
        int count = 0;
        for (SearchIndexAdapter adapter : adapters) {
            try {
                List<SearchableItem> items = adapter.fetchAll(communityId);
                for (SearchableItem item : items) {
                    index(item);
                    count++;
                }
            } catch (Exception e) {
                log.error("Reindex failed for module {} community {}: {}",
                        adapter.getModule(), communityId, e.getMessage());
            }
        }
        return count;
    }

    private SearchResultDto mapRow(Object[] row) {
        String tagsStr = row[7] != null ? (String) row[7] : "";
        List<String> tags = tagsStr.isBlank() ? Collections.emptyList() : Arrays.asList(tagsStr.split(","));

        String metadataStr = row[13] != null ? (String) row[13] : "{}";
        Map<String, Object> metadata = parseJson(metadataStr);

        double rank = row[17] != null ? ((Number) row[17]).doubleValue() : 0.0;

        return new SearchResultDto(
                String.valueOf(row[0]),      // id
                (String) row[2],             // module
                (String) row[4],             // title
                (String) row[5],             // subtitle
                (String) row[6],             // description
                (String) row[8],             // image_url
                tags,
                (String) row[9],             // deep_link_type
                (String) row[10],            // deep_link_id
                rank,
                metadata
        );
    }

    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}
