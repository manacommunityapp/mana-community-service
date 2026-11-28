package com.manacommunity.api.guard.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.guard.dto.CreateHandoverRequest;
import com.manacommunity.api.guard.entity.HandoverItem;
import com.manacommunity.api.guard.entity.ShiftHandover;
import com.manacommunity.api.guard.repository.ShiftHandoverRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        return acknowledge(id, user, Set.of());
    }

    /**
     * Enforces forced acknowledgment of every unresolved handover item (e.g., visitors
     * still inside, uncollected parcels at gate, offline hardware alerts) before a shift
     * handover can transition to ACKNOWLEDGED.
     */
    public ShiftHandover acknowledge(Long id, AppUser user, Set<Long> acknowledgedOpenItemIds) {
        ShiftHandover h = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ShiftHandover", id));

        Set<Long> ackIds = acknowledgedOpenItemIds != null ? acknowledgedOpenItemIds : Set.of();
        List<HandoverItem> unresolved = h.getItems() == null ? List.of() : h.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getResolvedBefore()))
                .filter(item -> item.getId() == null || !ackIds.contains(item.getId()))
                .toList();

        if (!unresolved.isEmpty()) {
            String summary = unresolved.stream()
                    .map(i -> (i.getCategory() != null ? i.getCategory() : "ITEM") + ": " + i.getDescription())
                    .collect(Collectors.joining("; "));
            throw new InvalidInputException(
                    "Cannot complete shift handover without acknowledging open items: " + summary);
        }

        h.setStatus("ACKNOWLEDGED");
        h.setAcknowledgedBy(user);
        return repo.save(h);
    }
}
