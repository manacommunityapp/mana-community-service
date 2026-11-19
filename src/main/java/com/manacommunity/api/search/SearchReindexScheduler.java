package com.manacommunity.api.search;

import com.manacommunity.api.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchReindexScheduler {

    private final SearchIndexService searchIndexService;
    private final CommunityRepository communityRepository;

    // Daily full reindex at 2am
    @Scheduled(cron = "0 0 2 * * *")
    public void reindexAll() {
        log.info("Starting nightly search reindex for all communities");
        communityRepository.findAll().forEach(community -> {
            try {
                int count = searchIndexService.reindexCommunity(community.getId());
                log.info("Reindexed {} items for community {}", count, community.getId());
            } catch (Exception e) {
                log.error("Reindex failed for community {}: {}", community.getId(), e.getMessage());
            }
        });
    }
}
