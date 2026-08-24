package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventRegistrationReportRowDto;
import com.manacommunity.api.events.dto.EventReportResponse;
import com.manacommunity.api.events.entity.*;
import com.manacommunity.api.events.repository.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventReportService {

    private final EventCommunityRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final EventPoojaUserRegistrationRepository poojaRegRepo;
    private final EventActivityRegistrationRepository activityRegRepo;
    private final EventBookingRegistrationRepository bookingRegRepo;
    private final EventMealRegistrationRepository mealRegRepo;
    private final EventVolunteerRepository volunteerRepo;
    private final EventDonationRepository donationRepo;
    private final EventSponsorRepository sponsorRepo;
    private final EventExpenseRepository expenseRepo;
    private final EventGalleryItemRepository galleryRepo;
    private final EventTaskRepository taskRepo;
    private final EventProgramRepository programRepo;

    @Transactional(readOnly = true)
    public EventReportResponse getEventReport(Long eventId) {
        EventCommunity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        long generalRegs = regRepo.findByEventId(eventId).size();
        List<EventPoojaUserRegistration> poojaList = poojaRegRepo.findByEventIdOrderByCreatedAtDesc(eventId);
        long poojaRegs = poojaList.stream().filter(p -> !"CANCELLED".equalsIgnoreCase(p.getStatus())).count();
        long activityRegs = activityRegRepo.countByProgramEventId(eventId);
        long bookingRegs = bookingRegRepo.countByMainEventIdAndStatusNot(eventId, "CANCELLED");
        long mealRegs = mealRegRepo.findByEventIdOrdered(eventId).size();

        long totalRegs = generalRegs + poojaRegs + activityRegs + bookingRegs + mealRegs;
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

        double poojaRevenue = poojaList.stream()
                .filter(p -> !"CANCELLED".equalsIgnoreCase(p.getStatus()) && p.getBookingFee() != null)
                .mapToDouble(EventPoojaUserRegistration::getBookingFee)
                .sum();

        long totalHeadcount = generalRegs
                + poojaList.stream().mapToLong(p -> p.getDevoteeCount() != null ? p.getDevoteeCount() : 1).sum()
                + mealRegRepo.findByEventIdOrdered(eventId).stream().mapToLong(m -> m.getHeadCount() != null ? m.getHeadCount() : 1).sum();

        long checkedInCount = regRepo.findByEventId(eventId).stream().filter(r -> Boolean.TRUE.equals(r.getCheckedIn())).count()
                + bookingRegRepo.findByMainEventIdOrderByCreatedAtDesc(eventId).stream().filter(b -> Boolean.TRUE.equals(b.getCheckedIn())).count();

        double totalRevenue = donations + sponsorships + poojaRevenue;

        return EventReportResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalRegistrations(totalRegs)
                .generalRegistrationsCount(generalRegs)
                .poojaRegistrationsCount(poojaRegs)
                .activityRegistrationsCount(activityRegs)
                .bookingRegistrationsCount(bookingRegs)
                .mealRegistrationsCount(mealRegs)
                .totalVolunteers(volunteers)
                .totalDonations(donations)
                .totalSponsorships(sponsorships)
                .totalExpenses(expenses)
                .poojaRevenue(poojaRevenue)
                .totalRevenue(totalRevenue)
                .netRevenue(totalRevenue - expenses)
                .totalGalleryItems(gallery)
                .totalTasks(tasks)
                .completedTasks(doneTasks)
                .totalPrograms(programs)
                .totalDevoteesHeadcount(totalHeadcount)
                .totalCheckedIn(checkedInCount)
                .build();
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationReportRowDto> getEventRegistrationsReport(Long eventId, String categoryFilter) {
        EventCommunity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        String cat = categoryFilter != null ? categoryFilter.trim().toLowerCase() : "all";
        List<EventRegistrationReportRowDto> rows = new ArrayList<>();

        // 1. General Event Registrations
        if ("all".equals(cat) || "general".equals(cat) || "event".equals(cat)) {
            List<EventRegistration> generalList = regRepo.findByEventId(eventId);
            for (EventRegistration r : generalList) {
                String userName = r.getUser() != null ? r.getUser().getFullName() : "Devotee";
                String email = r.getUser() != null ? r.getUser().getEmail() : "";
                String phone = r.getUser() != null ? r.getUser().getPhone() : "";
                rows.add(EventRegistrationReportRowDto.builder()
                        .id("GEN-" + r.getId())
                        .regCode("EVT-" + r.getId())
                        .category("General Event")
                        .activityTitle(event.getTitle())
                        .participantName(userName)
                        .email(email)
                        .phone(phone)
                        .gotram(r.getGotram())
                        .devoteeCount(1)
                        .eventDate(event.getStartDate() != null ? event.getStartDate().toString() : "")
                        .eventTime(event.getStartTime() != null ? event.getStartTime().toString() : "")
                        .venue(event.getVenue())
                        .bookingFee(0.0)
                        .paymentStatus("FREE")
                        .paymentMethod(r.getPaymentMethod() != null ? r.getPaymentMethod() : "Online")
                        .transactionId(r.getTransactionId())
                        .status(r.getStatus() != null ? r.getStatus().name() : "CONFIRMED")
                        .checkedIn(Boolean.TRUE.equals(r.getCheckedIn()))
                        .checkedInAt(r.getCheckedInAt())
                        .registeredAt(r.getRegisteredAt())
                        .build());
            }
        }

        // 2. Pooja Seva Registrations
        if ("all".equals(cat) || "pooja".equals(cat)) {
            List<EventPoojaUserRegistration> poojaList = poojaRegRepo.findByEventIdOrderByCreatedAtDesc(eventId);
            for (EventPoojaUserRegistration p : poojaList) {
                rows.add(EventRegistrationReportRowDto.builder()
                        .id("POOJA-" + p.getId())
                        .regCode(p.getRegCode())
                        .category("Pooja Seva")
                        .activityTitle(p.getPoojaSlotName() != null ? p.getPoojaSlotName() : "Pooja Seva")
                        .participantName(p.getParticipantName())
                        .email(p.getEmail())
                        .phone(p.getPhone())
                        .gotram(p.getGotram())
                        .devoteeCount(p.getDevoteeCount() != null ? p.getDevoteeCount() : 1)
                        .attendingDevotees(p.getAttendingDevotees())
                        .eventDate(p.getPoojaSlotDate())
                        .eventTime(p.getPoojaSlotTime())
                        .venue(p.getVenue())
                        .mandap(p.getMandap())
                        .panditName(p.getPanditName())
                        .bookingFee(p.getBookingFee() != null ? p.getBookingFee() : 0.0)
                        .paymentStatus(p.getPaymentStatus() != null ? p.getPaymentStatus() : "PAID")
                        .paymentMethod(p.getPaymentMethod())
                        .transactionId(p.getTransactionId())
                        .status(p.getStatus() != null ? p.getStatus() : "CONFIRMED")
                        .checkedIn(false)
                        .prasadamMode(p.getPrasadamMode())
                        .notes(p.getNotes())
                        .registeredAt(p.getCreatedAt())
                        .build());
            }
        }

        // 3. Cultural & EventCompetition Activities
        if ("all".equals(cat) || "activity".equals(cat) || "cultural".equals(cat) || "competition".equals(cat)) {
            List<EventActivityRegistration> actList = activityRegRepo.findByProgramEventId(eventId);
            for (EventActivityRegistration a : actList) {
                String name = a.getPrimaryName() != null ? a.getPrimaryName() : (a.getUser() != null ? a.getUser().getFullName() : "Participant");
                String email = a.getPrimaryEmail() != null ? a.getPrimaryEmail() : (a.getUser() != null ? a.getUser().getEmail() : "");
                String phone = a.getPrimaryPhone() != null ? a.getPrimaryPhone() : (a.getUser() != null ? a.getUser().getPhone() : "");
                String title = a.getProgram() != null ? a.getProgram().getTitle() : "Event Program";
                String type = a.getProgram() != null && a.getProgram().getProgramType() != null ? a.getProgram().getProgramType() : "Activity";

                rows.add(EventRegistrationReportRowDto.builder()
                        .id("ACT-" + a.getId())
                        .regCode("ACT-" + (a.getIdempotencyKey() != null ? a.getIdempotencyKey() : a.getId()))
                        .category("Activity / " + type)
                        .activityTitle(title)
                        .participantName(name)
                        .email(email)
                        .phone(phone)
                        .gotram(a.getGotram())
                        .devoteeCount(a.getHeadCount() != null ? a.getHeadCount() : 1)
                        .attendingDevotees(a.getParticipantData())
                        .eventDate(a.getProgram() != null && a.getProgram().getDayDate() != null ? a.getProgram().getDayDate().toString() : "")
                        .eventTime(a.getProgram() != null && a.getProgram().getStartTime() != null ? a.getProgram().getStartTime().toString() : "")
                        .venue(a.getProgram() != null ? a.getProgram().getVenue() : "")
                        .bookingFee(0.0)
                        .paymentStatus("FREE")
                        .status(a.getStatus() != null ? a.getStatus().name() : "CONFIRMED")
                        .checkedIn(false)
                        .registeredAt(a.getRegisteredAt())
                        .build());
            }
        }

        // 4. Booking & Stall Registrations
        if ("all".equals(cat) || "booking".equals(cat) || "stall".equals(cat)) {
            List<EventBookingRegistration> bkgList = bookingRegRepo.findByMainEventIdOrderByCreatedAtDesc(eventId);
            for (EventBookingRegistration b : bkgList) {
                rows.add(EventRegistrationReportRowDto.builder()
                        .id("BKG-" + b.getId())
                        .regCode(b.getRegCode())
                        .category(b.getCategory() != null ? b.getCategory() : "Booking")
                        .activityTitle(b.getActivityTitle())
                        .participantName(b.getParticipantName())
                        .email(b.getUser() != null ? b.getUser().getEmail() : "")
                        .phone(b.getUser() != null ? b.getUser().getPhone() : "")
                        .gotram(b.getGotram())
                        .devoteeCount(b.getDevoteeCount() != null ? b.getDevoteeCount() : 1)
                        .attendingDevotees(b.getAttendingDevotees())
                        .eventDate(b.getEventDate())
                        .eventTime(b.getEventTime())
                        .venue(b.getVenue())
                        .bookingFee(b.getBookingFee() != null ? b.getBookingFee() : 0.0)
                        .paymentStatus(b.getPaymentStatus() != null ? b.getPaymentStatus() : "PAID")
                        .paymentMethod(b.getPaymentMethod())
                        .transactionId(b.getTransactionId())
                        .status(b.getStatus() != null ? b.getStatus() : "CONFIRMED")
                        .checkedIn(Boolean.TRUE.equals(b.getCheckedIn()))
                        .checkedInAt(b.getCheckedInAt())
                        .registeredAt(b.getCreatedAt())
                        .build());
            }
        }

        // 5. Meals & Annadanam Registrations
        if ("all".equals(cat) || "meal".equals(cat) || "food".equals(cat)) {
            List<EventMealRegistration> mealList = mealRegRepo.findByEventIdOrdered(eventId);
            for (EventMealRegistration m : mealList) {
                String userName = m.getUser() != null ? m.getUser().getFullName() : "Devotee";
                String email = m.getUser() != null ? m.getUser().getEmail() : "";
                String phone = m.getUser() != null ? m.getUser().getPhone() : "";
                String mealTitle = (m.getMealType() != null ? m.getMealType().name() : "Meal")
                        + " (" + (m.getDietaryPref() != null ? m.getDietaryPref().name() : "Veg") + ")";

                rows.add(EventRegistrationReportRowDto.builder()
                        .id("MEAL-" + m.getId())
                        .regCode("MEAL-" + m.getId())
                        .category("Food & Meals")
                        .activityTitle(mealTitle)
                        .participantName(userName)
                        .email(email)
                        .phone(phone)
                        .devoteeCount(m.getHeadCount() != null ? m.getHeadCount() : 1)
                        .eventDate(m.getMealDate() != null ? m.getMealDate().toString() : "")
                        .notes(m.getAllergies() != null ? "Allergies: " + m.getAllergies() : "")
                        .bookingFee(0.0)
                        .paymentStatus("FREE")
                        .status("CONFIRMED")
                        .checkedIn(false)
                        .registeredAt(m.getCreatedAt())
                        .build());
            }
        }

        // 6. Volunteers
        if ("all".equals(cat) || "volunteer".equals(cat)) {
            List<EventVolunteer> volList = volunteerRepo.findByEventIdOrderByCreatedAtDesc(eventId);
            for (EventVolunteer v : volList) {
                String volName = v.getUserName() != null ? v.getUserName() : (v.getUser() != null ? v.getUser().getFullName() : "Volunteer");
                String email = v.getUser() != null ? v.getUser().getEmail() : "";
                String phone = v.getUser() != null ? v.getUser().getPhone() : "";
                String roleTitle = (v.getRole() != null ? v.getRole() : "Volunteer") + (v.getZone() != null ? " - Zone: " + v.getZone() : "");

                rows.add(EventRegistrationReportRowDto.builder()
                        .id("VOL-" + v.getId())
                        .regCode("VOL-" + v.getId())
                        .category("Volunteer")
                        .activityTitle(roleTitle)
                        .participantName(volName)
                        .email(email)
                        .phone(phone)
                        .devoteeCount(1)
                        .eventDate(event.getStartDate() != null ? event.getStartDate().toString() : "")
                        .eventTime(v.getShift())
                        .venue(v.getZone())
                        .bookingFee(0.0)
                        .paymentStatus("FREE")
                        .status(v.getStatus() != null ? v.getStatus() : "ACTIVE")
                        .checkedIn(v.getCheckInTime() != null)
                        .checkedInAt(v.getCheckInTime())
                        .registeredAt(v.getCheckInTime())
                        .build());
            }
        }

        // Sort by registeredAt descending
        rows.sort(Comparator.comparing(EventRegistrationReportRowDto::getRegisteredAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    @Transactional(readOnly = true)
    public byte[] generateRegistrationsCsv(Long eventId, String categoryFilter) {
        List<EventRegistrationReportRowDto> rows = getEventRegistrationsReport(eventId, categoryFilter);

        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        sb.append('\ufeff');

        // Headers
        sb.append("Reg Code,Category,Activity / Pooja / Seva,Participant Name,Phone,Email,Gotram,Devotee Count,Attending Devotees,Event Date,Event Time,Venue,Mandap,Pandit Name,Booking Fee (INR),Payment Status,Payment Method,Transaction ID,Status,Checked In,Prasadam Mode,Registered At,Notes\r\n");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (EventRegistrationReportRowDto r : rows) {
            sb.append(escapeCsv(r.getRegCode())).append(",");
            sb.append(escapeCsv(r.getCategory())).append(",");
            sb.append(escapeCsv(r.getActivityTitle())).append(",");
            sb.append(escapeCsv(r.getParticipantName())).append(",");
            sb.append(escapeCsv(r.getPhone())).append(",");
            sb.append(escapeCsv(r.getEmail())).append(",");
            sb.append(escapeCsv(r.getGotram())).append(",");
            sb.append(r.getDevoteeCount() != null ? r.getDevoteeCount() : 1).append(",");
            sb.append(escapeCsv(r.getAttendingDevotees())).append(",");
            sb.append(escapeCsv(r.getEventDate())).append(",");
            sb.append(escapeCsv(r.getEventTime())).append(",");
            sb.append(escapeCsv(r.getVenue())).append(",");
            sb.append(escapeCsv(r.getMandap())).append(",");
            sb.append(escapeCsv(r.getPanditName())).append(",");
            sb.append(r.getBookingFee() != null ? String.format("%.2f", r.getBookingFee()) : "0.00").append(",");
            sb.append(escapeCsv(r.getPaymentStatus())).append(",");
            sb.append(escapeCsv(r.getPaymentMethod())).append(",");
            sb.append(escapeCsv(r.getTransactionId())).append(",");
            sb.append(escapeCsv(r.getStatus())).append(",");
            sb.append(Boolean.TRUE.equals(r.getCheckedIn()) ? "YES" : "NO").append(",");
            sb.append(escapeCsv(r.getPrasadamMode())).append(",");
            sb.append(escapeCsv(r.getRegisteredAt() != null ? r.getRegisteredAt().format(dtf) : "")).append(",");
            sb.append(escapeCsv(r.getNotes())).append("\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        String clean = val.replace("\"", "\"\"");
        return "\"" + clean + "\"";
    }
}