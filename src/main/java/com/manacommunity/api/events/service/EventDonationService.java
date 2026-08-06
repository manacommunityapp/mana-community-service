package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventDonationRequest;
import com.manacommunity.api.events.dto.EventDonationResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventDonation;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventDonationRepository;
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
public class EventDonationService {

    private final EventDonationRepository donationRepo;
    private final CommunityEventRepository eventRepo;

    @Transactional(readOnly = true)
    public List<EventDonationResponse> getByEvent(Long eventId) {
        return donationRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventDonationResponse> getByCommunity(Long communityId) {
        return donationRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventDonationResponse create(EventDonationRequest req, AppUser user, Community community) {
        CommunityEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + req.getEventId()));

        EventDonation donation = EventDonation.builder()
                .event(event)
                .donorName(req.getDonorName())
                .donorEmail(req.getDonorEmail())
                .donorPhone(req.getDonorPhone())
                .amount(req.getAmount())
                .paymentMethod(parseEnumOrDefault(EventDonation.PaymentMethod.class, req.getPaymentMethod(), EventDonation.PaymentMethod.CASH))
                .transactionRef(req.getTransactionRef())
                .note(req.getNote())
                .anonymous(req.isAnonymous())
                .community(community)
                .recordedBy(user)
                .build();
        return toResponse(donationRepo.save(donation));
    }

    @Transactional
    public EventDonationResponse update(Long id, EventDonationRequest req) {
        EventDonation d = donationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + id));
        d.setDonorName(req.getDonorName());
        d.setDonorEmail(req.getDonorEmail());
        d.setDonorPhone(req.getDonorPhone());
        d.setAmount(req.getAmount());
        d.setPaymentMethod(parseEnumOrDefault(EventDonation.PaymentMethod.class, req.getPaymentMethod(), d.getPaymentMethod()));
        d.setTransactionRef(req.getTransactionRef());
        d.setNote(req.getNote());
        d.setAnonymous(req.isAnonymous());
        return toResponse(donationRepo.save(d));
    }

    @Transactional
    public void delete(Long id) {
        donationRepo.deleteById(id);
    }

    private EventDonationResponse toResponse(EventDonation d) {
        return EventDonationResponse.builder()
                .id(d.getId())
                .eventId(d.getEvent().getId())
                .eventTitle(d.getEvent().getTitle())
                .donorName(d.getDonorName())
                .donorEmail(d.getDonorEmail())
                .donorPhone(d.getDonorPhone())
                .amount(d.getAmount())
                .paymentMethod(d.getPaymentMethod().name())
                .transactionRef(d.getTransactionRef())
                .note(d.getNote())
                .anonymous(d.isAnonymous())
                .recordedByName(d.getRecordedBy().getFullName())
                .createdAt(d.getCreatedAt() != null ? d.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        if (value == null || value.isBlank()) return def;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return def; }
    }
}
