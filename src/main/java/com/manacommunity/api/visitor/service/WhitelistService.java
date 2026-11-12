package com.manacommunity.api.visitor.service;

import com.manacommunity.api.visitor.dto.WhitelistRequest;
import com.manacommunity.api.visitor.entity.WhitelistEntry;
import com.manacommunity.api.visitor.repository.WhitelistRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WhitelistService {

    private final WhitelistRepository repo;

    public List<WhitelistEntry> getMyWhitelist(AppUser user) {
        return repo.findByResidentId(user.getId());
    }

    public WhitelistEntry add(WhitelistRequest req, AppUser user) {
        return repo.save(WhitelistEntry.builder()
                .resident(user).community(user.getCommunity())
                .name(req.getName()).phone(req.getPhone())
                .category(req.getCategory() != null ? req.getCategory() : "GUEST")
                .vehicleNumber(req.getVehicleNumber()).notes(req.getNotes())
                .isActive(true).build());
    }

    public WhitelistEntry update(Long id, WhitelistRequest req, AppUser user) {
        WhitelistEntry e = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (!e.getResident().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        e.setName(req.getName());
        e.setPhone(req.getPhone());
        if (req.getCategory() != null) e.setCategory(req.getCategory());
        e.setVehicleNumber(req.getVehicleNumber());
        e.setNotes(req.getNotes());
        return repo.save(e);
    }

    public void remove(Long id, AppUser user) {
        WhitelistEntry e = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (!e.getResident().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        repo.delete(e);
    }

    public WhitelistEntry toggleStatus(Long id, boolean isActive, AppUser user) {
        WhitelistEntry e = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (!e.getResident().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        e.setIsActive(isActive);
        return repo.save(e);
    }
}
