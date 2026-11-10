package com.manacommunity.api.visitor.service;

import com.manacommunity.api.visitor.dto.CreateGroupInviteRequest;
import com.manacommunity.api.visitor.entity.GroupGuest;
import com.manacommunity.api.visitor.entity.GroupInvite;
import com.manacommunity.api.visitor.repository.GroupInviteRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupInviteService {

    private final GroupInviteRepository repo;

    public List<GroupInvite> getMyInvites(AppUser user) {
        return repo.findByHostIdOrderByCreatedAtDesc(user.getId());
    }

    public GroupInvite create(CreateGroupInviteRequest req, AppUser user) {
        GroupInvite invite = GroupInvite.builder()
                .host(user).community(user.getCommunity())
                .title(req.getTitle())
                .eventDate(req.getEventDate() != null ? LocalDate.parse(req.getEventDate()) : null)
                .passCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("ACTIVE").build();
        if (req.getGuests() != null) {
            req.getGuests().forEach(g -> invite.getGuests().add(GroupGuest.builder()
                    .groupInvite(invite).name(g.getName())
                    .phone(g.getPhone()).vehicleNumber(g.getVehicleNumber()).build()));
        }
        return repo.save(invite);
    }

    public void cancel(Long id, AppUser user) {
        GroupInvite invite = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (!invite.getHost().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        invite.setStatus("CANCELLED");
        repo.save(invite);
    }

    public GroupInvite addGuest(Long id, CreateGroupInviteRequest.GuestDto g, AppUser user) {
        GroupInvite invite = repo.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        if (!invite.getHost().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        invite.getGuests().add(GroupGuest.builder()
                .groupInvite(invite).name(g.getName())
                .phone(g.getPhone()).vehicleNumber(g.getVehicleNumber()).build());
        return repo.save(invite);
    }
}
