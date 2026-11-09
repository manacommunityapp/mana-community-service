package com.manacommunity.api.sports.service;
import com.manacommunity.api.sports.dto.SportsScheduleResponse.*;

import com.manacommunity.api.sports.dto.SportsScheduleResponse;
import com.manacommunity.api.sports.model.SportsPlayerCategory;
import com.manacommunity.api.sports.model.SportsEvent;
import com.manacommunity.api.sports.model.SportsEventRegistration;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SportsScheduleService {

    private final SportsEventService eventService;

    @Transactional(readOnly = true)
    public List<EventListItem> getOpenEvents(Long communityId) {
        return eventService.getOpenEvents(communityId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventListItem> getMyEvents(Long userId) {
        return eventService.getMyEvents(userId).stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventListItem> getAllOpenEvents() {
        return eventService.getAllOpenEvents().stream()
                .map(this::toListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationListItem> getMyRegistrations(Long userId) {
        return eventService.getUserRegistrations(userId).stream()
                .map(this::toRegistrationItem)
                .toList();
    }

    private EventListItem toListItem(SportsEvent e) {
        String sportName = e.getSport() != null ? e.getSport().getName() : null;
        String venueName = e.getVenue() != null ? e.getVenue().getName() : null;
        String venueCity = e.getVenue() != null ? e.getVenue().getCity() : null;
        String categoryName = firstCategoryName(e.getCategories());
        String status = e.getStatus() != null ? e.getStatus().name() : null;
        String auctionStatus = e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null;

        return new EventListItem(
                e.getId(),
                e.getUuid() != null ? e.getUuid().toString() : null,
                e.getName(),
                sportName,
                venueName,
                venueCity,
                e.getEventDateStart(),
                e.getEventDateEnd(),
                status,
                auctionStatus,
                e.getFormat() != null ? e.getFormat().stream().map(Enum::name).toList() : java.util.List.of(),
                categoryName,
                e.getMaxParticipants(),
                e.getStartTime()
        );
    }

    private RegistrationListItem toRegistrationItem(SportsEventRegistration r) {
        SportsEvent event = r.getEvent();
        String eventName = event != null ? event.getName() : null;
        Long eventId = event != null ? event.getId() : null;
        String sportName = (event != null && event.getSport() != null) ? event.getSport().getName() : null;
        String categoryName = r.getCategory() != null ? r.getCategory().getName() : null;

        Long partnerUserId = r.getPartner() != null ? r.getPartner().getId() : null;
        Long partnerFamilyMemberId = r.getPartnerFamilyMember() != null ? r.getPartnerFamilyMember().getId() : null;
        String pName = null;
        if (r.getPartnerFamilyMember() != null) {
            pName = r.getPartnerFamilyMember().getName();
        } else if (r.getPartner() != null) {
            pName = r.getPartner().getFullName();
        }
        String pPhone = r.getPartner() != null ? r.getPartner().getPhone() : null;
        String pFlat = r.getPartner() != null
                ? com.manacommunity.api.sports.util.SportsFormatUtils.formatBlockAndFlat(r.getPartner().getBlock(), r.getPartner().getFlatNo())
                : null;
        if (pFlat == null && r.getPartnerFamilyMember() != null && r.getPartnerFamilyMember().getUser() != null) {
            pFlat = com.manacommunity.api.sports.util.SportsFormatUtils.formatBlockAndFlat(
                    r.getPartnerFamilyMember().getUser().getBlock(),
                    r.getPartnerFamilyMember().getUser().getFlatNo());
        }
        if (pPhone == null && r.getPartnerFamilyMember() != null && r.getPartnerFamilyMember().getUser() != null) {
            pPhone = r.getPartnerFamilyMember().getUser().getPhone();
        }
        String pStatus = r.getPartnerConfirmationStatus() != null ? r.getPartnerConfirmationStatus().name() : null;

        String formattedFlat = r.getUser() != null
                ? com.manacommunity.api.sports.util.SportsFormatUtils.formatBlockAndFlat(
                        r.getUser().getBlock(),
                        r.getFlatNumber() != null && !r.getFlatNumber().isBlank() ? r.getFlatNumber() : r.getUser().getFlatNo())
                : com.manacommunity.api.sports.util.SportsFormatUtils.formatBlockAndFlat(null, r.getFlatNumber());

        return new RegistrationListItem(
                r.getId(),
                r.getStatus() != null ? r.getStatus().name() : null,
                eventId,
                eventName,
                sportName,
                categoryName,
                r.getAge(),
                formattedFlat,
                r.getRegisteredAt(),
                r.getCaptainNomination(),
                r.getCaptainConfirmation(),
                r.getProposedTeamName(),
                r.getPlayerName(),
                r.getRelation(),
                r.getFamilyMember() != null ? r.getFamilyMember().getId() : null,
                event != null ? event.getEventDateStart() : null,
                event != null ? event.getEventDateEnd() : null,
                event != null ? event.getStartTime() : null,
                event != null && event.getVenue() != null ? event.getVenue().getName() : null,
                event != null && event.getVenue() != null ? event.getVenue().getCity() : null,
                partnerUserId,
                partnerFamilyMemberId,
                pName,
                pPhone,
                pFlat,
                pStatus,
                pStatus
        );
    }

    private String firstCategoryName(Set<SportsPlayerCategory> categories) {
        if (categories == null || categories.isEmpty()) return null;
        return categories.iterator().next().getName();
    }
}
