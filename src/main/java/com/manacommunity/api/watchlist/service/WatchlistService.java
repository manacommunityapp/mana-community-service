package com.manacommunity.api.watchlist.service;

import com.manacommunity.api.watchlist.dto.WatchlistRequest;
import com.manacommunity.api.watchlist.entity.WatchlistEntry;
import com.manacommunity.api.watchlist.repository.WatchlistRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public List<WatchlistEntry> getWatchlist(AppUser user) {
        return watchlistRepository.findByCommunityIdAndStatusNot(user.getCommunity().getId(), "REMOVED");
    }

    public WatchlistEntry add(WatchlistRequest req, AppUser user) {
        return watchlistRepository.save(WatchlistEntry.builder()
                .community(user.getCommunity())
                .addedBy(user)
                .name(req.getName())
                .phone(req.getPhone())
                .vehicleNumber(req.getVehicleNumber())
                .photo(req.getPhoto())
                .reason(req.getReason())
                .category(req.getCategory() != null ? req.getCategory() : "SUSPICIOUS")
                .notes(req.getNotes())
                .status("ACTIVE")
                .build());
    }

    public WatchlistEntry update(Long id, WatchlistRequest req, AppUser user) {
        WatchlistEntry entry = watchlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Watchlist entry not found"));
        entry.setName(req.getName());
        entry.setPhone(req.getPhone());
        entry.setVehicleNumber(req.getVehicleNumber());
        if (req.getPhoto() != null) entry.setPhoto(req.getPhoto());
        entry.setReason(req.getReason());
        if (req.getCategory() != null) entry.setCategory(req.getCategory());
        entry.setNotes(req.getNotes());
        return watchlistRepository.save(entry);
    }

    public WatchlistEntry updateStatus(Long id, String status, AppUser user) {
        WatchlistEntry entry = watchlistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Watchlist entry not found"));
        entry.setStatus(status);
        return watchlistRepository.save(entry);
    }
}
