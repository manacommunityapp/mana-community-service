package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventReportResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.repository.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventReportService {

    private final CommunityEventRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final EventVolunteerRepository volunteerRepo;
    private final EventDonationRepository donationRepo;
    private final EventSponsorRepository sponsorRepo;
    private final EventExpenseRepository expenseRepo;
    private final EventGalleryItemRepository galleryRepo;
    private final EventTaskRepository taskRepo;
    private final EventProgramRepository programRepo;

    @Transactional(readOnly = true)
    public EventReportResponse getEventReport(Long eventId) {
        CommunityEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        long regs = regRepo.findByEventId(eventId).size();
        long volunteers = volunteerRepo.countByEventId(eventId);
        double donations = donationRepo.sumAmountByEvent(eventId);
        double expenses = expenseRepo.sumAmountByEvent(eventId);
        long gallery = galleryRepo.countByEventId(eventId);
        long tasks = taskRepo.findByEventIdOrderByDueDateAsc(eventId).size();
        long doneTasks = tasks - taskRepo.countByEventIdAndDoneFalse(eventId);
        long programs = programRepo.findByEventIdOrderBySortOrderAscStartTimeAsc(eventId).size();

        double sponsorships = 0;
        var sponsors = sponsorRepo.findByEventIdOrderByTierAscNameAsc(eventId);
        for (var s : sponsors) {
            if (s.getAmountReceived() != null) sponsorships += s.getAmountReceived();
        }

        return EventReportResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalRegistrations(regs)
                .totalVolunteers(volunteers)
                .totalDonations(donations)
                .totalSponsorships(sponsorships)
                .totalExpenses(expenses)
                .netRevenue(donations + sponsorships - expenses)
                .totalGalleryItems(gallery)
                .totalTasks(tasks)
                .completedTasks(doneTasks)
                .totalPrograms(programs)
                .build();
    }
}
