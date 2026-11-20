package com.manacommunity.api.communityops.watchlist;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistEntryRepository repo;

    @Transactional
    public WatchlistEntry add(Long communityId, Long addedBy, WatchlistEntryDto req) {
        return repo.save(WatchlistEntry.builder()
            .communityId(communityId)
            .type(req.type())
            .name(req.name())
            .identifier(req.identifier())
            .reason(req.reason())
            .severity(req.severity() != null ? req.severity() : WatchlistSeverity.CAUTION)
            .addedBy(addedBy)
            .active(true)
            .expiresAt(req.expiresAt())
            .build());
    }

    public List<WatchlistEntry> check(Long communityId, String identifier) {
        return repo.findByCommunityIdAndIdentifierIgnoreCaseAndActiveTrue(communityId, identifier);
    }

    @Transactional
    public void deactivate(Long id, Long adminId) {
        repo.findById(id).ifPresent(e -> {
            e.setActive(false);
            repo.save(e);
        });
    }

    public List<WatchlistEntry> getAll(Long communityId) {
        return repo.findByCommunityIdAndActiveTrue(communityId);
    }
}
