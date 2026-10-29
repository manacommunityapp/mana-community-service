package com.manacommunity.api.broadcast.service;

import com.manacommunity.api.broadcast.dto.SendBroadcastRequest;
import com.manacommunity.api.broadcast.entity.Broadcast;
import com.manacommunity.api.broadcast.repository.BroadcastRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;

    public List<Broadcast> getBroadcasts(AppUser user) {
        return broadcastRepository.findByCommunityIdOrderByCreatedAtDesc(user.getCommunity().getId());
    }

    public Broadcast send(SendBroadcastRequest req, AppUser user) {
        Broadcast broadcast = Broadcast.builder()
                .community(user.getCommunity())
                .createdBy(user)
                .title(req.getTitle())
                .message(req.getMessage())
                .category(req.getCategory() != null ? req.getCategory() : "GENERAL")
                .priority(req.getPriority() != null ? req.getPriority() : "NORMAL")
                .target(req.getTarget() != null ? req.getTarget() : "ALL")
                .status(req.getScheduledAt() != null ? "SCHEDULED" : "SENT")
                .scheduledAt(req.getScheduledAt() != null ? LocalDateTime.parse(req.getScheduledAt()) : null)
                .build();
        return broadcastRepository.save(broadcast);
    }

    public void delete(Long id, AppUser user) {
        Broadcast broadcast = broadcastRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Broadcast not found"));
        if (!broadcast.getCommunity().getId().equals(user.getCommunity().getId())) {
            throw new RuntimeException("Not authorized");
        }
        broadcastRepository.delete(broadcast);
    }
}
