package com.manacommunity.api.guard.service;

import com.manacommunity.api.guard.dto.CreateHandoverRequest;
import com.manacommunity.api.guard.entity.HandoverItem;
import com.manacommunity.api.guard.entity.ShiftHandover;
import com.manacommunity.api.guard.repository.ShiftHandoverRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShiftHandoverService {

    private final ShiftHandoverRepository repo;

    public List<ShiftHandover> getTodayHandovers(AppUser user) {
        Long cid = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (cid == null) return List.of();
        return repo.findByCommunityIdAndShiftDate(cid, LocalDate.now());
    }

    public List<ShiftHandover> getHistory(AppUser user) {
        Long cid = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (cid == null) return List.of();
        return repo.findByCommunityIdOrderByCreatedAtDesc(cid);
    }

    public ShiftHandover create(CreateHandoverRequest req, AppUser user) {
        ShiftHandover h = ShiftHandover.builder()
                .createdByUser(user).community(user.getCommunity())
                .shiftDate(LocalDate.now())
                .shiftType(req.getShiftType() != null ? req.getShiftType() : "MORNING")
                .notes(req.getNotes()).status("PENDING").build();
        if (req.getItems() != null) {
            req.getItems().forEach(i -> h.getItems().add(HandoverItem.builder()
                    .handover(h).category(i.getCategory())
                    .description(i.getDescription())
                    .priority(i.getPriority() != null ? i.getPriority() : "MEDIUM")
                    .resolvedBefore(Boolean.TRUE.equals(i.getResolvedBefore())).build()));
        }
        return repo.save(h);
    }

    public ShiftHandover acknowledge(Long id, AppUser user) {
        ShiftHandover h = repo.findById(id).orElseThrow(() -> new RuntimeException("Handover not found"));
        h.setStatus("ACKNOWLEDGED");
        h.setAcknowledgedBy(user);
        return repo.save(h);
    }
}
