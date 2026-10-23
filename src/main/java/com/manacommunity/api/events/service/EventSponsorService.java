package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventSponsorRequest;
import com.manacommunity.api.events.dto.EventSponsorResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventSponsor;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventSponsorRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventSponsorService {

    private final EventSponsorRepository sponsorRepo;
    private final EventCommunityRepository eventRepo;

    @Transactional(readOnly = true)
    public List<EventSponsorResponse> getByEvent(Long eventId) {
        return sponsorRepo.findByEventIdOrderByTierAscNameAsc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventSponsorResponse> getByCommunity(Long communityId) {
        return sponsorRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventSponsorResponse create(EventSponsorRequest req, AppUser user, Community community) {
        EventCommunity event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        EventSponsor sponsor = EventSponsor.builder()
                .event(event)
                .name(req.getName())
                .sponsorTier(parseEnumOrDefault(EventSponsor.SponsorTier.class, req.getTier(), EventSponsor.SponsorTier.SILVER))
                .amountPledged(req.getAmountPledged())
                .amountReceived(req.getAmountReceived())
                .logoUrl(req.getLogoUrl())
                .contactName(req.getContactName())
                .contactPhone(req.getContactPhone())
                .contactEmail(req.getContactEmail())
                .sponsorStatus(parseEnumOrDefault(EventSponsor.SponsorStatus.class, req.getStatus(), EventSponsor.SponsorStatus.PENDING))
                .community(community)
                .createdBy(user)
                .build();
        return toResponse(sponsorRepo.save(sponsor));
    }

    @Transactional
    public EventSponsorResponse update(Long id, EventSponsorRequest req) {
        EventSponsor s = sponsorRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sponsor", id));
        s.setName(req.getName());
        s.setSponsorTier(parseEnumOrDefault(EventSponsor.SponsorTier.class, req.getTier(), s.getSponsorTier()));
        s.setAmountPledged(req.getAmountPledged());
        s.setAmountReceived(req.getAmountReceived());
        s.setLogoUrl(req.getLogoUrl());
        s.setContactName(req.getContactName());
        s.setContactPhone(req.getContactPhone());
        s.setContactEmail(req.getContactEmail());
        s.setSponsorStatus(parseEnumOrDefault(EventSponsor.SponsorStatus.class, req.getStatus(), s.getSponsorStatus()));
        return toResponse(sponsorRepo.save(s));
    }

    @Transactional
    public void delete(Long id) {
        sponsorRepo.deleteById(id);
    }

    private EventSponsorResponse toResponse(EventSponsor s) {
        return EventSponsorResponse.builder()
                .id(s.getId())
                .eventId(s.getEvent().getId())
                .eventTitle(s.getEvent().getTitle())
                .name(s.getName())
                .tier(s.getSponsorTier().name())
                .amountPledged(s.getAmountPledged())
                .amountReceived(s.getAmountReceived())
                .logoUrl(s.getLogoUrl())
                .contactName(s.getContactName())
                .contactPhone(s.getContactPhone())
                .contactEmail(s.getContactEmail())
                .status(s.getSponsorStatus().name())
                .createdAt(s.getCreatedAt() != null ? s.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
