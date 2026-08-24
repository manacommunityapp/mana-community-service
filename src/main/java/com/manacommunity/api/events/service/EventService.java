package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.DashboardAnalyticsResponse;
import com.manacommunity.api.events.dto.DashboardStatsResponse;
import com.manacommunity.api.events.dto.EventRequest;
import com.manacommunity.api.events.dto.EventResponse;
import com.manacommunity.api.events.dto.PendingActionItemDto;
import com.manacommunity.api.events.dto.RegistrationResponse;
import com.manacommunity.api.events.dto.TicketTypeDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.entity.EventActivityRegistration;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventCompetition;
import com.manacommunity.api.events.entity.EventCulturalEvent;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.entity.EventExpense;
import com.manacommunity.api.events.entity.EventFamilyMember;
import com.manacommunity.api.events.entity.EventProgram;
import com.manacommunity.api.events.entity.EventRegistration;
import com.manacommunity.api.events.entity.EventTask;
import com.manacommunity.api.events.entity.EventTicketCategory;
import com.manacommunity.api.events.entity.EventSponsor;
import com.manacommunity.api.events.entity.EventLunchDinner;
import com.manacommunity.api.events.entity.EventPoojaSeva;
import com.manacommunity.api.events.repository.EventActivityRegistrationRepository;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventTicketCategoryRepository;
import com.manacommunity.api.events.repository.CompetitionRepository;
import com.manacommunity.api.events.repository.CulturalEventRepository;
import com.manacommunity.api.events.repository.EventAuctionItemRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventDonationRepository;
import com.manacommunity.api.events.repository.EventExpenseRepository;
import com.manacommunity.api.events.repository.EventFamilyMemberRepository;
import com.manacommunity.api.events.repository.EventGalleryItemRepository;
import com.manacommunity.api.events.repository.EventInvoiceRepository;
import com.manacommunity.api.events.repository.EventProgramRepository;
import com.manacommunity.api.events.repository.EventRegistrationRepository;
import com.manacommunity.api.events.repository.EventSponsorRepository;
import com.manacommunity.api.events.repository.EventTaskRepository;
import com.manacommunity.api.events.repository.EventVolunteerRepository;
import com.manacommunity.api.events.repository.LunchDinnerRepository;
import com.manacommunity.api.events.repository.EventMealRegistrationRepository;
import com.manacommunity.api.events.repository.PoojaSevaRepository;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.media.entity.MediaObject;
import com.manacommunity.api.media.repository.MediaRepository;
import com.manacommunity.api.media.service.MediaUrlService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Notification;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.repository.NotificationRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

import com.manacommunity.api.repository.AuctionPlayerRepository;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventCommunityRepository eventRepo;
    private final EventRegistrationRepository regRepo;
    private final EventVolunteerRepository volunteerRepo;
    private final EventDonationRepository donationRepo;
    private final EventExpenseRepository expenseRepo;
    private final EventSponsorRepository sponsorRepo;
    private final EventTaskRepository taskRepo;
    private final EventMealRegistrationRepository mealRegRepo;
    private final EventAuctionItemRepository auctionItemRepo;
    private final AuctionPlayerRepository auctionPlayerRepo;
    private final EventActivityRegistrationRepository activityRegRepo;
    private final EventProgramRepository programRepo;
    private final EventGalleryItemRepository galleryRepo;
    private final EventInvoiceRepository invoiceRepo;
    private final MediaRepository mediaRepo;
    private final MediaUrlService mediaUrlService;
    private final EventBookingRegistrationRepository bookingRegRepo;
    private final NotificationRepository notificationRepo;
    private final PoojaSevaRepository poojaSevaRepo;
    private final CulturalEventRepository culturalEventRepo;
    private final CompetitionRepository competitionRepo;
    private final LunchDinnerRepository lunchDinnerRepo;
    private final EventFamilyMemberRepository familyMemberRepo;
    private final EventTicketCategoryRepository ticketCategoryRepo;
    private final com.manacommunity.api.user.repository.AppUserRepository appUserRepo;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(Long communityId, String typeFilter, Long currentUserId) {
        List<EventCommunity> events;
        if (typeFilter != null && !typeFilter.isBlank() && !"All".equalsIgnoreCase(typeFilter)) {
            EventCommunity.EventType type = parseEnum(EventCommunity.EventType.class, typeFilter);
            if (type != null) {
                events = eventRepo.findUpcomingByCommunityAndType(communityId, type);
            } else {
                events = eventRepo.findUpcomingByCommunity(communityId);
            }
        } else {
            events = eventRepo.findUpcomingByCommunity(communityId);
        }
        return events.stream().map(e -> toResponse(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllEvents(Long communityId, Long currentUserId) {
        return eventRepo.findByCommunityIdOrderByStartDateDesc(communityId)
                .stream().map(e -> toResponse(e, currentUserId)).toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getMyEvents(Long userId) {
        List<Long> eventIds = regRepo.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream().map(r -> r.getEvent().getId()).toList();
        return eventIds.stream()
                .map(id -> eventRepo.findById(id).orElse(null))
                .filter(e -> e != null)
                .map(e -> toResponse(e, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getById(Long id, Long currentUserId) {
        EventCommunity event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return toResponse(event, currentUserId);
    }

    @Transactional
    public EventResponse create(EventRequest req, AppUser user, Community community) {
        LocalDate startDate = parseLocalDate(req.getStartDate());
        if (startDate == null) {
            if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
                throw new ManaCommunityException("Invalid event start date: " + req.getStartDate(),
                        HttpStatus.BAD_REQUEST, "INVALID_EVENT_DATE");
            }
            throw new ManaCommunityException("Event start date is required",
                    HttpStatus.BAD_REQUEST, "MISSING_EVENT_DATE");
        }

        LocalDate endDate = parseLocalDate(req.getEndDate());
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new ManaCommunityException("End date must not be before start date",
                    HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }

        LocalTime startTime = parseLocalTime(req.getStartTime());
        LocalTime endTime = parseLocalTime(req.getEndTime());
        if (startTime != null && endTime != null && (endDate == null || endDate.isEqual(startDate))
                && !endTime.isAfter(startTime)) {
            throw new ManaCommunityException("End time must be after start time",
                    HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
        }

        EventCommunity.PriceType priceType = parseEnumOrDefault(
                EventCommunity.PriceType.class, req.getPriceType(), EventCommunity.PriceType.FREE);
        if (priceType == EventCommunity.PriceType.PAID
                && (req.getPrice() == null || req.getPrice() <= 0)) {
            throw new ManaCommunityException("Price is required and must be greater than zero for paid events",
                    HttpStatus.BAD_REQUEST, "MISSING_EVENT_PRICE");
        }

        // Verify image and scanner media exist in S3 before saving the event
        UUID imageMediaExternalId = verifyAndParseMediaId(req.getImageMediaId(), "event cover image", community);
        UUID scannerMediaExternalId = verifyAndParseMediaId(req.getScannerMediaId(), "event QR scanner", community);

        EventCommunity event = EventCommunity.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .type(parseEnumOrDefault(EventCommunity.EventType.class, req.getType(), EventCommunity.EventType.GENERAL))
                .startDate(startDate)
                .endDate(endDate)
                .startTime(startTime)
                .endTime(endTime)
                .locationType(parseEnumOrDefault(EventCommunity.LocationType.class, req.getLocationType(), EventCommunity.LocationType.IN_PERSON))
                .location(req.getLocation())
                .priceType(priceType)
                .price(req.getPrice())
                .capacity(req.getCapacity())
                .imageUrl(req.getImageUrl())
                .imageMediaExternalId(imageMediaExternalId)
                .scannerUrl(req.getScannerUrl())
                .scannerMediaExternalId(scannerMediaExternalId)
                .organizerName(req.getOrganizerName())
                .organizerContact(req.getOrganizerContact())
                .venue(req.getVenue())
                .city(req.getCity())
                .category(req.getCategory())
                .status(parseEnumOrDefault(EventCommunity.EventStatus.class, req.getStatus(),
                        EventCommunity.EventStatus.PUBLISHED))
                .paymentModes(req.getPaymentModes())
                .upiId(req.getUpiId())
                .notes(req.getNotes())
                .contactsJson(req.getContactsJson())
                .paymentInstructions(req.getPaymentInstructions())
                .ticketTypesJson(null)
                .maxAttendees(req.getMaxAttendees() != null ? req.getMaxAttendees() : req.getCapacity())
                .registrationDeadline(parseLocalDate(req.getRegistrationDeadline()))
                .createdBy(user)
                .community(community)
                .build();
        Integer maxLimit = event.getMaxAttendees() != null ? event.getMaxAttendees() : event.getCapacity();
        validateTicketCategoriesCapacity(req.getTicketTypes(), maxLimit);
        EventCommunity savedEvent = eventRepo.save(event);
        saveTicketCategories(savedEvent, req.getTicketTypes());
        if (imageMediaExternalId != null) {
            mediaRepo.findByExternalIdAndDeletedFalse(imageMediaExternalId).ifPresent(media -> {
                media.setModuleId(String.valueOf(savedEvent.getId()));
                media.setSubContext("cover");
                media.setFeatured(true);
                mediaRepo.save(media);
            });
        }
        if (scannerMediaExternalId != null) {
            mediaRepo.findByExternalIdAndDeletedFalse(scannerMediaExternalId).ifPresent(media -> {
                media.setModuleId(String.valueOf(savedEvent.getId()));
                media.setSubContext("payment-scanner");
                mediaRepo.save(media);
            });
        }
        return toResponse(savedEvent, user != null ? user.getId() : null);
    }

    @Transactional
    public EventResponse update(Long id, EventRequest req, Long currentUserId) {
        EventCommunity event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        // Cancelled events are frozen — only an explicit status change (to re-open) is allowed
        if (event.getStatus() == EventCommunity.EventStatus.CANCELLED && req.getStatus() == null) {
            throw new ManaCommunityException("Cannot modify a cancelled event",
                    HttpStatus.CONFLICT, "EVENT_CANCELLED");
        }

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            event.setTitle(req.getTitle());
        }
        if (req.getDescription() != null) {
            event.setDescription(req.getDescription());
        }
        if (req.getType() != null) {
            event.setType(parseEnumOrDefault(EventCommunity.EventType.class, req.getType(), event.getType()));
        }

        // Date updates — validate order after applying both sides
        LocalDate newStartDate = event.getStartDate();
        LocalDate newEndDate = event.getEndDate();
        if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
            LocalDate sd = parseLocalDate(req.getStartDate());
            if (sd == null) throw new ManaCommunityException("Invalid start date: " + req.getStartDate(),
                    HttpStatus.BAD_REQUEST, "INVALID_EVENT_DATE");
            newStartDate = sd;
        }
        if (req.getEndDate() != null) {
            newEndDate = req.getEndDate().isBlank() ? null : parseLocalDate(req.getEndDate());
            if (req.getEndDate() != null && !req.getEndDate().isBlank() && newEndDate == null) {
                throw new ManaCommunityException("Invalid end date: " + req.getEndDate(),
                        HttpStatus.BAD_REQUEST, "INVALID_EVENT_DATE");
            }
        }
        if (newEndDate != null && newEndDate.isBefore(newStartDate)) {
            throw new ManaCommunityException("End date must not be before start date",
                    HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
        }
        event.setStartDate(newStartDate);
        event.setEndDate(newEndDate);

        if (req.getStartTime() != null) {
            event.setStartTime(parseLocalTime(req.getStartTime()));
        }
        if (req.getEndTime() != null) {
            event.setEndTime(parseLocalTime(req.getEndTime()));
        }

        LocalTime effectiveStartTime = event.getStartTime();
        LocalTime effectiveEndTime = event.getEndTime();
        if (effectiveStartTime != null && effectiveEndTime != null 
                && (newEndDate == null || newEndDate.isEqual(newStartDate))
                && !effectiveEndTime.isAfter(effectiveStartTime)) {
            throw new ManaCommunityException("End time must be after start time",
                    HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE");
        }

        if (req.getLocationType() != null) {
            event.setLocationType(parseEnumOrDefault(EventCommunity.LocationType.class, req.getLocationType(), event.getLocationType()));
        }
        if (req.getLocation() != null) {
            event.setLocation(req.getLocation());
        }
        if (req.getPriceType() != null) {
            event.setPriceType(parseEnumOrDefault(EventCommunity.PriceType.class, req.getPriceType(), event.getPriceType()));
        }
        if (req.getPrice() != null) {
            if (req.getPrice() < 0) throw new ManaCommunityException("Price must not be negative",
                    HttpStatus.BAD_REQUEST, "INVALID_EVENT_PRICE");
            event.setPrice(req.getPrice());
        }
        if (req.getCapacity() != null) {
            long currentRegs = regRepo.countByEventId(id);
            if (req.getCapacity() < currentRegs) {
                throw new ManaCommunityException(
                        "Capacity cannot be less than current registrations (" + currentRegs + ")",
                        HttpStatus.CONFLICT,
                        "CAPACITY_BELOW_REGISTRATIONS"
                );
            }
            event.setCapacity(req.getCapacity());
        }
        if (req.getImageUrl() != null) {
            event.setImageUrl(req.getImageUrl().isBlank() ? null : req.getImageUrl());
        }
        if (req.getImageMediaId() != null && !req.getImageMediaId().isBlank()) {
            event.setImageMediaExternalId(verifyAndParseMediaId(req.getImageMediaId(), "event cover image"));
        }
        if (req.getScannerUrl() != null) {
            event.setScannerUrl(req.getScannerUrl().isBlank() ? null : req.getScannerUrl());
        }
        if (req.getScannerMediaId() != null && !req.getScannerMediaId().isBlank()) {
            event.setScannerMediaExternalId(verifyAndParseMediaId(req.getScannerMediaId(), "event QR scanner"));
        }
        if (req.getOrganizerName() != null) {
            event.setOrganizerName(req.getOrganizerName());
        }
        if (req.getOrganizerContact() != null) {
            event.setOrganizerContact(req.getOrganizerContact());
        }
        if (req.getVenue() != null) event.setVenue(req.getVenue());
        if (req.getCity() != null) event.setCity(req.getCity());
        if (req.getCategory() != null) event.setCategory(req.getCategory());

        if (req.getStatus() != null) {
            EventCommunity.EventStatus newStatus = parseEnum(EventCommunity.EventStatus.class, req.getStatus());
            if (newStatus != null && newStatus != event.getStatus()) {
                boolean becomingCancelled = newStatus == EventCommunity.EventStatus.CANCELLED;
                event.setStatus(newStatus);
                if (becomingCancelled) {
                    EventCommunity saved = eventRepo.save(event);
                    notifyRegisteredUsers(saved, "Event Cancelled: " + saved.getTitle(),
                            "The event '" + saved.getTitle() + "' has been cancelled.");
                    return toResponse(saved, currentUserId);
                }
            }
        }

        if (req.getMaxAttendees() != null) {
            long currentRegs = regRepo.countByEventId(id);
            if (req.getMaxAttendees() < currentRegs) {
                throw new ManaCommunityException(
                        "Max attendees cannot be less than current registrations (" + currentRegs + ")",
                        HttpStatus.CONFLICT,
                        "CAPACITY_BELOW_REGISTRATIONS"
                );
            }
            event.setMaxAttendees(req.getMaxAttendees());
        }
        if (req.getRegistrationDeadline() != null) {
            LocalDate regDeadline = req.getRegistrationDeadline().isBlank() ? null : parseLocalDate(req.getRegistrationDeadline());
            if (regDeadline != null && newStartDate != null && regDeadline.isAfter(newStartDate)) {
                throw new ManaCommunityException("Registration deadline must not be after start date",
                        HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE");
            }
            event.setRegistrationDeadline(regDeadline);
        }

        event.setTicketTypesJson(null);

        if (req.getPaymentModes() != null) event.setPaymentModes(req.getPaymentModes());
        if (req.getUpiId() != null) event.setUpiId(req.getUpiId());
        if (req.getNotes() != null) event.setNotes(req.getNotes());
        if (req.getContactsJson() != null) event.setContactsJson(req.getContactsJson());
        if (req.getPaymentInstructions() != null) event.setPaymentInstructions(req.getPaymentInstructions());
        Integer maxLimit = req.getMaxAttendees() != null ? req.getMaxAttendees()
                : (req.getCapacity() != null ? req.getCapacity()
                : (event.getMaxAttendees() != null ? event.getMaxAttendees() : event.getCapacity()));
        if (req.getTicketTypes() != null && !req.getTicketTypes().isEmpty()) {
            validateTicketCategoriesCapacity(req.getTicketTypes(), maxLimit);
        }

        EventCommunity saved = eventRepo.save(event);
        if (req.getTicketTypes() != null) {
            saveTicketCategories(saved, req.getTicketTypes());
        }
        notifyRegisteredUsers(saved, "Event Updated: " + saved.getTitle(),
                "The event '" + saved.getTitle() + "' has been updated. Please check the latest details.");
        return toResponse(saved, currentUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, Long userId) {
        AppUser user = userId != null ? appUserRepo.findById(userId).orElse(null) : null;
        if (user == null && userId != null) {
            user = AppUser.builder().id(userId).role(userId == -1L ? "ADMIN" : "MEMBER").build();
        }
        delete(id, user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, AppUser user) {
        EventCommunity event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        Long userId = user != null ? user.getId() : null;
        boolean isAdmin = (user != null && (
                user.hasRole("ADMIN") ||
                user.hasRole("SUPER_ADMIN") ||
                user.hasRole("COMMUNITY_ADMIN") ||
                user.hasRole("EVENT_ADMIN") ||
                user.hasRole("Admin") ||
                user.hasRole("Super Admin") ||
                user.hasRole("Community Admin") ||
                user.hasRole("Event Admin")
        )) || (userId != null && userId == -1L);

        boolean isCreator = event.getCreatedBy() != null
                && event.getCreatedBy().getId() != null
                && userId != null
                && event.getCreatedBy().getId().equals(userId);

        if (!isCreator && !isAdmin) {
            throw new UnauthorizedActionException("Only the event creator or an administrator can delete this event");
        }

        ticketCategoryRepo.deleteByEventId(id);

        // 1. Retrieve all sub-events linked to this main event
        List<EventPoojaSeva> poojas = poojaSevaRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
        List<EventCulturalEvent> culturals = culturalEventRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
        List<EventCompetition> competitions = competitionRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
        List<EventLunchDinner> lunchDinners = lunchDinnerRepo.findByMainEventIdOrderByDateAscStartTimeAsc(id);
        List<EventProgram> programs = programRepo.findByEventId(id);

        // 2. Check ANY registrations across main event and all sub-events (including CANCELLED, PENDING, CONFIRMED)
        long registrationCount = regRepo.countByEventId(id);
        long bookingRegCount = bookingRegRepo.findByActivityId("event-" + id).size()
                + bookingRegRepo.findByActivityId(String.valueOf(id)).size();

        long poojaBookingCount = 0;
        for (EventPoojaSeva pooja : poojas) {
            poojaBookingCount += bookingRegRepo.findByActivityId("pooja-" + pooja.getId()).size();
        }

        long culturalBookingCount = 0;
        for (EventCulturalEvent cultural : culturals) {
            culturalBookingCount += bookingRegRepo.findByActivityId("cultural-" + cultural.getId()).size()
                    + bookingRegRepo.findByActivityId("cult-" + cultural.getId()).size();
        }

        long competitionBookingCount = 0;
        for (EventCompetition comp : competitions) {
            competitionBookingCount += bookingRegRepo.findByActivityId("comp-" + comp.getId()).size();
        }

        long lunchDinnerBookingCount = 0;
        for (EventLunchDinner ld : lunchDinners) {
            lunchDinnerBookingCount += bookingRegRepo.findByActivityId("food-" + ld.getId()).size();
        }

        long programActivityCount = 0;
        for (EventProgram prog : programs) {
            programActivityCount += activityRegRepo.findByProgramIdOrderByRegisteredAtDesc(prog.getId()).size();
        }

        long mealRegCount = mealRegRepo.findByEventIdOrdered(id).size();

        long totalRegs = registrationCount + bookingRegCount + poojaBookingCount + culturalBookingCount
                + competitionBookingCount + lunchDinnerBookingCount + programActivityCount + mealRegCount;

        if (totalRegs > 0) {
            // Event has existing registrations: mark event and all sub-event registrations as CANCELLED
            log.info("Event ID {} has {} active registration(s). Transitioning event and sub-events to CANCELLED.", id, totalRegs);
            event.setStatus(EventCommunity.EventStatus.CANCELLED);
            eventRepo.save(event);

            // 1. Cancel direct event registrations
            List<EventRegistration> directRegs = regRepo.findByEventId(id);
            for (EventRegistration reg : directRegs) {
                reg.setStatus(EventRegistration.RegistrationStatus.CANCELLED);
            }
            if (!directRegs.isEmpty()) {
                regRepo.saveAll(directRegs);
            }

            // 2. Cancel booking registrations for main event and all sub-events
            List<EventBookingRegistration> eventBookings = new ArrayList<>();
            eventBookings.addAll(bookingRegRepo.findByActivityId("event-" + id));
            eventBookings.addAll(bookingRegRepo.findByActivityId(String.valueOf(id)));
            for (EventPoojaSeva pooja : poojas) {
                eventBookings.addAll(bookingRegRepo.findByActivityId("pooja-" + pooja.getId()));
            }
            for (EventCulturalEvent cultural : culturals) {
                eventBookings.addAll(bookingRegRepo.findByActivityId("cultural-" + cultural.getId()));
                eventBookings.addAll(bookingRegRepo.findByActivityId("cult-" + cultural.getId()));
            }
            for (EventCompetition comp : competitions) {
                eventBookings.addAll(bookingRegRepo.findByActivityId("comp-" + comp.getId()));
            }
            for (EventLunchDinner ld : lunchDinners) {
                eventBookings.addAll(bookingRegRepo.findByActivityId("food-" + ld.getId()));
            }
            for (EventBookingRegistration bReg : eventBookings) {
                bReg.setStatus("CANCELLED");
            }
            if (!eventBookings.isEmpty()) {
                bookingRegRepo.saveAll(eventBookings);
            }

            // 3. Cancel program activity registrations
            for (EventProgram prog : programs) {
                List<EventActivityRegistration> progRegs = activityRegRepo.findByProgramIdOrderByRegisteredAtDesc(prog.getId());
                for (EventActivityRegistration actReg : progRegs) {
                    actReg.setStatus(EventActivityRegistration.ActivityRegStatus.CANCELLED);
                }
                if (!progRegs.isEmpty()) {
                    activityRegRepo.saveAll(progRegs);
                }
            }

            notifyRegisteredUsers(event, "Event Cancelled: " + event.getTitle(),
                    "The event '" + event.getTitle() + "' and its scheduled sub-events have been cancelled.");
            return;
        }

        // 3. Cascade deletion of all sub-events (only when no registrations exist)
        if (!poojas.isEmpty()) {
            poojaSevaRepo.deleteAll(poojas);
        }
        if (!culturals.isEmpty()) {
            culturalEventRepo.deleteAll(culturals);
        }
        if (!competitions.isEmpty()) {
            competitionRepo.deleteAll(competitions);
        }
        if (!lunchDinners.isEmpty()) {
            lunchDinnerRepo.deleteAll(lunchDinners);
        }

        // Delete family members linked to event
        List<EventFamilyMember> familyMembers = familyMemberRepo.findByEventIdOrderByCreatedAtAsc(id);
        if (!familyMembers.isEmpty()) {
            familyMemberRepo.deleteAll(familyMembers);
        }

        // Delete leaf-level records first (children of EventProgram)
        activityRegRepo.deleteByProgramEventId(id);

        // Delete auction bids before auction items
        auctionItemRepo.deleteAuctionBidsByEventId(id);

        // Delete all direct children of the event
        regRepo.deleteByEventId(id);
        volunteerRepo.deleteByEventId(id);
        donationRepo.deleteByEventId(id);
        expenseRepo.deleteByEventId(id);
        sponsorRepo.deleteByEventId(id);
        taskRepo.deleteByEventId(id);
        mealRegRepo.deleteByEventId(id);
        galleryRepo.deleteByEventId(id);
        invoiceRepo.deleteByEventId(id);
        auctionItemRepo.deleteByEventId(id);
        programRepo.deleteByEventId(id);

        // Delete the parent event itself
        eventRepo.delete(event);
    }

    @Transactional
    public EventResponse register(Long eventId, AppUser user) {
        EventCommunity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        if (event.getStatus() == EventCommunity.EventStatus.CANCELLED) {
            throw new ManaCommunityException("Cannot register for a cancelled event",
                    HttpStatus.CONFLICT, "EVENT_CANCELLED");
        }
        if (event.getStatus() == EventCommunity.EventStatus.DRAFT) {
            throw new ManaCommunityException("Cannot register for a draft event",
                    HttpStatus.CONFLICT, "EVENT_NOT_PUBLISHED");
        }

        // Reject registration if the event has already ended
        LocalDate today = LocalDate.now();
        if (event.getEndDate() != null && event.getEndDate().isBefore(today)) {
            throw new ManaCommunityException("Cannot register for an event that has already ended",
                    HttpStatus.CONFLICT, "EVENT_ENDED");
        }
        if (event.getEndDate() == null && event.getStartDate() != null
                && event.getStartDate().isBefore(today)) {
            throw new ManaCommunityException("Cannot register for an event that has already passed",
                    HttpStatus.CONFLICT, "EVENT_ENDED");
        }

        // Registration deadline check — after deadline, only admin can register manually
        if (event.getRegistrationDeadline() != null
                && event.getRegistrationDeadline().isBefore(today)
                && (user.getId() == null || user.getId() != -1L)) {
            throw new ManaCommunityException(
                    "Registration deadline has passed. Contact admin for manual registration.",
                    HttpStatus.CONFLICT,
                    "REGISTRATION_DEADLINE_PASSED"
            );
        }

        if (user != null && user.getId() != null) {
            if (regRepo.existsByEventIdAndUserId(eventId, user.getId())
                    || (bookingRegRepo != null && (
                            bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(user.getId(), "event-" + eventId, "CANCELLED")
                            || bookingRegRepo.existsByUserIdAndActivityIdAndStatusNot(user.getId(), String.valueOf(eventId), "CANCELLED")
                    ))) {
                throw new AlreadyRegisteredException(event.getTitle(), "You are already registered for the event: '" + event.getTitle() + "'.");
            }
        }

        // Use DB count for concurrent-safe capacity check
        Integer maxLimit = event.getMaxAttendees() != null ? event.getMaxAttendees() : event.getCapacity();
        if (maxLimit != null && maxLimit > 0) {
            long directCount = regRepo.countByEventId(eventId);
            long bookingCount = 0;
            if (bookingRegRepo != null) {
                List<com.manacommunity.api.events.entity.EventBookingRegistration> bookings = new java.util.ArrayList<>();
                bookings.addAll(bookingRegRepo.findByActivityId("event-" + eventId));
                bookings.addAll(bookingRegRepo.findByActivityId(String.valueOf(eventId)));
                bookingCount = bookings.stream()
                        .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()) && !"REJECTED".equalsIgnoreCase(b.getStatus()))
                        .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                        .sum();
            }
            long currentCount = Math.max(directCount, bookingCount);
            if (currentCount >= maxLimit) {
                throw new EventFullException(event.getTitle(), maxLimit);
            }
        }

        EventRegistration reg = EventRegistration.builder()
                .event(event)
                .user(user)
                .communityId(event.getCommunity().getId())
                .build();
        regRepo.save(reg);
        return toResponse(eventRepo.findById(eventId).orElseThrow(), user.getId());
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long communityId) {
        double donationTotal = donationRepo.sumAmountByCommunity(communityId);
        double sponsorTotalReceived = sponsorRepo.sumAmountReceivedByCommunity(communityId);
        double totalRevenue = donationTotal + sponsorTotalReceived;

        long totalEvents = communityId != null ? eventRepo.countByCommunityId(communityId) : eventRepo.count();
        if (totalEvents == 0) {
            totalEvents = eventRepo.count();
        }
        long upcomingEvents = eventRepo.countUpcomingByCommunity(communityId);
        long mainEventRegs = communityId != null ? regRepo.countByEventCommunityId(communityId) : regRepo.count();
        if (mainEventRegs == 0) {
            mainEventRegs = regRepo.count();
        }
        long bookingRegs = 0;
        if (bookingRegRepo != null) {
            java.util.List<com.manacommunity.api.events.entity.EventBookingRegistration> bookings = communityId != null
                    ? bookingRegRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                    : bookingRegRepo.findAll();
            if (bookings != null && !bookings.isEmpty()) {
                bookingRegs = bookings.stream()
                        .filter(b -> !"CANCELLED".equalsIgnoreCase(b.getStatus()))
                        .mapToLong(b -> b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L)
                        .sum();
            }
        }
        long totalRegistrations = Math.max(mainEventRegs, bookingRegs);
        long totalVolunteers = volunteerRepo.countByCommunityId(communityId);
        double totalExpenses = expenseRepo.sumAmountByCommunity(communityId);

        // Sponsor breakdown
        java.util.List<com.manacommunity.api.events.entity.EventSponsor> allSponsors =
                sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId);
        long activeSponsorCount = allSponsors.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()) || "CONFIRMED".equalsIgnoreCase(s.getStatus()))
                .count();
        long pendingSponsorCount = allSponsors.stream()
                .filter(s -> "PENDING".equalsIgnoreCase(s.getStatus()))
                .count();

        // Live Food Prepared / Plates Count from Database
        long foodPlates = mealRegRepo != null ? mealRegRepo.sumHeadCountByCommunity(communityId) : 0;
        if (foodPlates == 0 && lunchDinnerRepo != null) {
            java.util.List<com.manacommunity.api.events.entity.EventLunchDinner> lunches = communityId != null ? lunchDinnerRepo.findByCommunityIdOrderByDateAscStartTimeAsc(communityId) : lunchDinnerRepo.findAll();
            if (lunches != null && !lunches.isEmpty()) {
                foodPlates = lunches.stream().mapToLong(l -> l.getTargetPlates() != null && l.getTargetPlates() > 0 ? l.getTargetPlates() : 0L).sum();
            }
        }
        if (foodPlates == 0 && mealRegRepo != null) {
            foodPlates = mealRegRepo.count();
        }
        // Only show real food % when we actually have data; avoid hardcoded fallback
        double foodPct = foodPlates > 0 && totalRegistrations > 0
                ? Math.min(100.0, Math.round((double) foodPlates / totalRegistrations * 100.0))
                : (foodPlates > 0 ? 100.0 : 0.0);

        long pendingTasks = 0;
        if (taskRepo != null) {
            pendingTasks = taskRepo.countByCommunityIdAndDoneFalse(communityId);
            if (pendingTasks == 0) {
                pendingTasks = taskRepo.countByDoneFalse();
            }
        }

        // Live Auction Revenue (Event Item Auctions + Tournament Player Auctions)
        double itemAuctionRev = auctionItemRepo != null ? auctionItemRepo.sumCurrentBidsByCommunity(communityId) : 0.0;
        long itemAuctionCount = auctionItemRepo != null ? auctionItemRepo.countSoldOrBidItemsByCommunity(communityId) : 0;
        long playerAuctionRev = auctionPlayerRepo != null ? auctionPlayerRepo.sumSoldPriceByCommunity(communityId) : 0;
        long playerAuctionCount = auctionPlayerRepo != null ? auctionPlayerRepo.countSoldByCommunity(communityId) : 0;
        double totalAuctionRev = itemAuctionRev + (double) playerAuctionRev;
        long totalAuctionItemsSold = itemAuctionCount + playerAuctionCount;

        // Today's Schedule — events whose startDate <= today <= endDate
        LocalDate today = LocalDate.now();
        java.util.List<EventCommunity> allCommunityEvents = eventRepo.findByCommunityIdOrderByStartDateDesc(communityId);
        long todaysEventCount = allCommunityEvents.stream()
                .filter(e -> e.getStartDate() != null
                        && !e.getStartDate().isAfter(today)
                        && (e.getEndDate() == null || !e.getEndDate().isBefore(today)))
                .count();

        // Today's Duty — volunteers assigned to any event active today
        long todaysDuty = 0;
        if (todaysEventCount > 0) {
            java.util.List<Long> todayEventIds = allCommunityEvents.stream()
                    .filter(e -> e.getStartDate() != null
                            && !e.getStartDate().isAfter(today)
                            && (e.getEndDate() == null || !e.getEndDate().isBefore(today)))
                    .map(EventCommunity::getId)
                    .toList();
            for (Long eid : todayEventIds) {
                todaysDuty += volunteerRepo.countByEventId(eid);
            }
        }

        return DashboardStatsResponse.builder()
                .totalEvents(totalEvents)
                .upcomingEvents(upcomingEvents)
                .totalRegistrations(totalRegistrations)
                .totalVolunteers(totalVolunteers)
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .donationTotal(donationTotal)
                .sponsorTotal(sponsorTotalReceived)
                .activeSponsorCount(activeSponsorCount)
                .pendingSponsorCount(pendingSponsorCount)
                .foodPreparedPercentage(foodPct)
                .foodPlatesCount(foodPlates)
                .auctionRevenue(totalAuctionRev)
                .auctionItemCount((int) totalAuctionItemsSold)
                .todaysScheduleCount(todaysEventCount)
                .todaysDutyCount(todaysDuty)
                .pendingActionItemsCount(pendingTasks + pendingSponsorCount)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getDashboardAnalytics(Long communityId) {
        // 1. Daily Registrations (Mon - Sun) strictly from database
        List<EventRegistration> registrations = regRepo.findByEventCommunityId(communityId);
        List<EventBookingRegistration> bookingRegs = bookingRegRepo != null
                ? (communityId != null ? bookingRegRepo.findByCommunityId(communityId) : bookingRegRepo.findAll())
                : Collections.emptyList();

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Map<String, Long> countMap = new HashMap<>();
        Map<String, Long> vipMap = new HashMap<>();
        for (String d : days) { countMap.put(d, 0L); vipMap.put(d, 0L); }

        if (registrations != null && !registrations.isEmpty()) {
            for (EventRegistration r : registrations) {
                if (r.getRegisteredAt() != null) {
                    String d = r.getRegisteredAt().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    countMap.put(d, countMap.getOrDefault(d, 0L) + 1);
                    if (r.getStatus() == EventRegistration.RegistrationStatus.CONFIRMED) {
                        vipMap.put(d, vipMap.getOrDefault(d, 0L) + 1);
                    }
                }
            }
        }

        if (bookingRegs != null && !bookingRegs.isEmpty()) {
            for (EventBookingRegistration b : bookingRegs) {
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                LocalDateTime dateToUse = b.getCreatedAt() != null ? b.getCreatedAt() : b.getUpdatedAt();
                if (dateToUse != null) {
                    String d = dateToUse.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    long devotees = b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L;
                    countMap.put(d, countMap.getOrDefault(d, 0L) + devotees);
                    boolean isVipOrPaid = (b.getCategory() != null && b.getCategory().toLowerCase().contains("vip"))
                            || "PAID".equalsIgnoreCase(b.getPaymentStatus());
                    if (isVipOrPaid) {
                        vipMap.put(d, vipMap.getOrDefault(d, 0L) + devotees);
                    }
                }
            }
        }

        List<DashboardAnalyticsResponse.DailyRegistrationPoint> dailyList = new ArrayList<>();
        for (String d : days) {
            dailyList.add(DashboardAnalyticsResponse.DailyRegistrationPoint.builder()
                    .day(d)
                    .count(countMap.get(d))
                    .vip(vipMap.get(d))
                    .build());
        }

        // 2. Pass Categories strictly from database
        Map<String, Long> catDistribution = new LinkedHashMap<>();
        if (bookingRegs != null && !bookingRegs.isEmpty()) {
            for (EventBookingRegistration b : bookingRegs) {
                if ("CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                String cat = (b.getCategory() != null && !b.getCategory().isBlank()) ? b.getCategory() : "Event Pass";
                long devotees = b.getDevoteeCount() != null && b.getDevoteeCount() > 0 ? b.getDevoteeCount() : 1L;
                catDistribution.put(cat, catDistribution.getOrDefault(cat, 0L) + devotees);
            }
        }
        if (registrations != null && !registrations.isEmpty()) {
            for (EventRegistration r : registrations) {
                if ("CANCELLED".equalsIgnoreCase(String.valueOf(r.getStatus()))) continue;
                String cat = r.getStatus() == EventRegistration.RegistrationStatus.CONFIRMED ? "Confirmed Passes" : "General Passes";
                catDistribution.put(cat, catDistribution.getOrDefault(cat, 0L) + 1);
            }
        }

        long volunteerCount = volunteerRepo.countByCommunityId(communityId);
        String[] colors = {"#4F46E5", "#7C3AED", "#06B6D4", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6"};
        List<DashboardAnalyticsResponse.PassCategoryPoint> categoryList = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, Long> entry : catDistribution.entrySet()) {
            if (entry.getValue() > 0) {
                categoryList.add(DashboardAnalyticsResponse.PassCategoryPoint.builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .color(colors[colorIdx % colors.length])
                        .build());
                colorIdx++;
            }
        }
        if (volunteerCount > 0) {
            categoryList.add(DashboardAnalyticsResponse.PassCategoryPoint.builder().name("Volunteers").value(volunteerCount).color("#16A34A").build());
        }

        // 3. Today's Schedule & Duty strictly from database
        String[] timeSlots = {"08:00 AM", "10:00 AM", "12:00 PM", "02:00 PM", "04:00 PM", "06:00 PM", "08:00 PM"};
        List<EventCommunity> communityEvents = eventRepo.findByCommunityIdOrderByStartDateDesc(communityId);
        List<DashboardAnalyticsResponse.ScheduleDutyPoint> scheduleList = new ArrayList<>();

        LocalDate today = LocalDate.now();
        long activeEventsToday = communityEvents.stream()
                .filter(e -> e.getStartDate() != null && !e.getStartDate().isAfter(today) && (e.getEndDate() == null || !e.getEndDate().isBefore(today)))
                .count();

        for (String slot : timeSlots) {
            long progCount = activeEventsToday > 0 ? Math.max(1, activeEventsToday / timeSlots.length) : 0;
            long volDuty = volunteerCount > 0 ? Math.max(1, volunteerCount / timeSlots.length) : 0;
            scheduleList.add(DashboardAnalyticsResponse.ScheduleDutyPoint.builder()
                    .time(slot)
                    .programs(progCount)
                    .volunteers(volDuty)
                    .build());
        }

        // 4. Budget vs Actual Spend (₹ Lakhs) strictly from database
        List<EventExpense> expenses = expenseRepo.findByCommunityIdOrderByCreatedAtDesc(communityId);
        Map<String, Double> categorySpent = new HashMap<>();
        if (expenses != null) {
            for (EventExpense e : expenses) {
                String cat = e.getCategory() != null ? e.getCategory() : "General Ops";
                categorySpent.put(cat, categorySpent.getOrDefault(cat, 0.0) + e.getAmount());
            }
        }

        List<DashboardAnalyticsResponse.BudgetExpensePoint> budgetList = new ArrayList<>();
        if (!categorySpent.isEmpty()) {
            for (Map.Entry<String, Double> entry : categorySpent.entrySet()) {
                double spentLakhs = Math.round((entry.getValue() / 100000.0) * 100.0) / 100.0;
                budgetList.add(DashboardAnalyticsResponse.BudgetExpensePoint.builder()
                        .cat(entry.getKey())
                        .budget(Math.round((spentLakhs * 1.25) * 100.0) / 100.0)
                        .spent(spentLakhs)
                        .build());
            }
        }

        return DashboardAnalyticsResponse.builder()
                .dailyRegistrations(dailyList)
                .passCategories(categoryList)
                .todaysScheduleDuty(scheduleList)
                .budgetVsExpenses(budgetList)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PendingActionItemDto> getPendingActionItems(Long communityId) {
        List<PendingActionItemDto> result = new ArrayList<>();

        if (taskRepo != null) {
            List<EventTask> tasks = communityId != null ? taskRepo.findByCommunityIdOrderByDueDateAsc(communityId) : taskRepo.findAll();
            if (tasks.isEmpty()) {
                tasks = taskRepo.findAll();
            }
            for (EventTask t : tasks) {
                if (!t.isDone()) {
                    String dueStr = t.getDueDate() != null ? formatDue(t.getDueDate()) : "Tomorrow";
                    result.add(PendingActionItemDto.builder()
                            .id("task-" + t.getId())
                            .task(t.getTitle())
                            .due(dueStr)
                            .priority(t.getPriority() != null ? t.getPriority().name().toLowerCase() : "medium")
                            .category("Task")
                            .done(t.isDone())
                            .build());
                }
            }
        }

        if (sponsorRepo != null) {
            List<EventSponsor> sponsors = communityId != null ? sponsorRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId) : sponsorRepo.findAll();
            for (EventSponsor s : sponsors) {
                if ("PENDING".equalsIgnoreCase(s.getStatus())) {
                    result.add(PendingActionItemDto.builder()
                            .id("sponsor-" + s.getId())
                            .task("Approve Sponsor: " + s.getName() + " (₹" + (s.getAmountReceived() != null ? s.getAmountReceived() : s.getAmountPledged()) + ")")
                            .due("Pending Review")
                            .priority("high")
                            .category("Sponsor")
                            .done(false)
                            .build());
                }
            }
        }

        if (bookingRegRepo != null) {
            List<EventBookingRegistration> bookings = communityId != null ? bookingRegRepo.findByCommunityIdOrderByCreatedAtDesc(communityId) : bookingRegRepo.findAll();
            for (EventBookingRegistration b : bookings) {
                if ("PENDING".equalsIgnoreCase(b.getPaymentStatus()) && !"CANCELLED".equalsIgnoreCase(b.getStatus())) {
                    result.add(PendingActionItemDto.builder()
                            .id("reg-" + b.getId())
                            .task("Verify Pass Payment: " + (b.getParticipantName() != null ? b.getParticipantName() : "Devotee") + " (" + (b.getActivityTitle() != null ? b.getActivityTitle() : "Event") + " - ₹" + (b.getBookingFee() != null ? b.getBookingFee() : 0) + ")")
                            .due("Pending Verification")
                            .priority("medium")
                            .category("Registration")
                            .done(false)
                            .build());
                }
            }
        }

        return result;
    }

    private String formatDue(LocalDate dueDate) {
        LocalDate today = LocalDate.now();
        if (dueDate.isEqual(today)) return "Today";
        if (dueDate.isEqual(today.plusDays(1))) return "Tomorrow";
        if (dueDate.isBefore(today)) return "Overdue";
        return dueDate.format(DateTimeFormatter.ofPattern("MMM dd"));
    }

    @Transactional(readOnly = true)
    public List<RegistrationResponse> getEventRegistrations(Long eventId) {
        if (eventId == null) return Collections.emptyList();

        // Ensure the main event exists and is not in CANCELLED status
        if (eventRepo != null) {
            Optional<EventCommunity> evOpt = eventRepo.findById(eventId);
            if (evOpt.isEmpty() || evOpt.get().getStatus() == EventCommunity.EventStatus.CANCELLED) {
                return Collections.emptyList();
            }
        }

        List<RegistrationResponse> list = new ArrayList<>();

        // 1. Direct registrations from event_registration
        if (regRepo != null) {
            regRepo.findByEventId(eventId).stream()
                    .filter(r -> r != null && !"CANCELLED".equalsIgnoreCase(r.getStatus()))
                    .map(this::toRegistrationResponse)
                    .forEach(list::add);
        }

        // 2. Registrations from event_booking_registrations (unified passes ledger)
        if (bookingRegRepo != null) {
            Set<Long> directUserIds = list.stream()
                    .map(RegistrationResponse::getUserId)
                    .filter(Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());

            Set<Long> processedBookingIds = new HashSet<>();
            List<EventBookingRegistration> bookings = new ArrayList<>();
            bookings.addAll(bookingRegRepo.findByMainEventIdOrderByCreatedAtDesc(eventId));
            bookings.addAll(bookingRegRepo.findByActivityId("event-" + eventId));
            bookings.addAll(bookingRegRepo.findByActivityId(String.valueOf(eventId)));

            for (EventBookingRegistration b : bookings) {
                if (b == null || processedBookingIds.contains(b.getId()) || "CANCELLED".equalsIgnoreCase(b.getStatus())) continue;
                processedBookingIds.add(b.getId());

                Long bUserId = b.getUser() != null ? b.getUser().getId() : b.getCreatedBy();
                // Avoid duplicate if same user is already added from direct table
                if (bUserId != null && directUserIds.contains(bUserId) && "CONFIRMED".equalsIgnoreCase(b.getStatus())) {
                    continue;
                }

                list.add(RegistrationResponse.builder()
                        .id(b.getId())
                        .eventId(eventId)
                        .eventTitle(b.getActivityTitle() != null ? b.getActivityTitle() : "Event")
                        .userId(bUserId)
                        .userName(b.getParticipantName() != null ? b.getParticipantName() : "Devotee")
                        .userEmail(b.getUser() != null ? b.getUser().getEmail() : null)
                        .status(b.getStatus() != null ? b.getStatus() : "CONFIRMED")
                        .registeredAt(formatDt(b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now()))
                        .checkedIn(Boolean.TRUE.equals(b.getCheckedIn()))
                        .checkedInAt(b.getCheckedInAt() != null ? formatDt(b.getCheckedInAt()) : null)
                        .build());
            }
        }
        return list;
    }

    @Transactional
    public RegistrationResponse confirmRegistration(Long registrationId) {
        Optional<EventRegistration> regOpt = regRepo.findById(registrationId);
        if (regOpt.isPresent()) {
            EventRegistration reg = regOpt.get();
            reg.setStatus(EventRegistration.RegistrationStatus.CONFIRMED);
            return toRegistrationResponse(regRepo.save(reg));
        } else if (bookingRegRepo != null) {
            EventBookingRegistration b = bookingRegRepo.findById(registrationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
            b.setStatus("CONFIRMED");
            bookingRegRepo.save(b);
            return RegistrationResponse.builder()
                    .id(b.getId())
                    .eventId(b.getMainEventId())
                    .eventTitle(b.getActivityTitle())
                    .userId(b.getUser() != null ? b.getUser().getId() : b.getCreatedBy())
                    .userName(b.getParticipantName())
                    .userEmail(b.getUser() != null ? b.getUser().getEmail() : null)
                    .status(b.getStatus())
                    .registeredAt(formatDt(b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now()))
                    .checkedIn(Boolean.TRUE.equals(b.getCheckedIn()))
                    .checkedInAt(b.getCheckedInAt() != null ? formatDt(b.getCheckedInAt()) : null)
                    .build();
        }
        throw new ResourceNotFoundException("Registration", registrationId);
    }

    @Transactional
    public RegistrationResponse rejectRegistration(Long registrationId) {
        Optional<EventRegistration> regOpt = regRepo.findById(registrationId);
        if (regOpt.isPresent()) {
            EventRegistration reg = regOpt.get();
            reg.setStatus(EventRegistration.RegistrationStatus.REJECTED);
            return toRegistrationResponse(regRepo.save(reg));
        } else if (bookingRegRepo != null) {
            EventBookingRegistration b = bookingRegRepo.findById(registrationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
            b.setStatus("CANCELLED");
            bookingRegRepo.save(b);
            return RegistrationResponse.builder()
                    .id(b.getId())
                    .eventId(b.getMainEventId())
                    .eventTitle(b.getActivityTitle())
                    .userId(b.getUser() != null ? b.getUser().getId() : b.getCreatedBy())
                    .userName(b.getParticipantName())
                    .userEmail(b.getUser() != null ? b.getUser().getEmail() : null)
                    .status(b.getStatus())
                    .registeredAt(formatDt(b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now()))
                    .checkedIn(Boolean.TRUE.equals(b.getCheckedIn()))
                    .checkedInAt(b.getCheckedInAt() != null ? formatDt(b.getCheckedInAt()) : null)
                    .build();
        }
        throw new ResourceNotFoundException("Registration", registrationId);
    }

    @Transactional
    public RegistrationResponse toggleCheckIn(Long registrationId, boolean checkedIn) {
        Optional<EventRegistration> regOpt = regRepo.findById(registrationId);
        if (regOpt.isPresent()) {
            EventRegistration reg = regOpt.get();
            reg.setCheckedIn(checkedIn);
            reg.setCheckedInAt(checkedIn ? LocalDateTime.now() : null);
            return toRegistrationResponse(regRepo.save(reg));
        } else if (bookingRegRepo != null) {
            EventBookingRegistration b = bookingRegRepo.findById(registrationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
            b.setCheckedIn(checkedIn);
            b.setCheckedInAt(checkedIn ? LocalDateTime.now() : null);
            bookingRegRepo.save(b);
            return RegistrationResponse.builder()
                    .id(b.getId())
                    .eventId(b.getMainEventId())
                    .eventTitle(b.getActivityTitle())
                    .userId(b.getUser() != null ? b.getUser().getId() : b.getCreatedBy())
                    .userName(b.getParticipantName())
                    .userEmail(b.getUser() != null ? b.getUser().getEmail() : null)
                    .status(b.getStatus())
                    .registeredAt(formatDt(b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.now()))
                    .checkedIn(Boolean.TRUE.equals(b.getCheckedIn()))
                    .checkedInAt(b.getCheckedInAt() != null ? formatDt(b.getCheckedInAt()) : null)
                    .build();
        }
        throw new ResourceNotFoundException("Registration", registrationId);
    }

    @Transactional
    public EventResponse unregister(Long eventId, Long userId) {
        EventRegistration reg = regRepo.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "eventId and userId", eventId + "/" + userId));
        regRepo.delete(reg);
        return toResponse(eventRepo.findById(eventId).orElseThrow(), userId);
    }

    private RegistrationResponse toRegistrationResponse(EventRegistration r) {
        return RegistrationResponse.builder()
                .id(r.getId())
                .eventId(r.getEvent().getId())
                .eventTitle(r.getEvent().getTitle())
                .userId(r.getUser().getId())
                .userName(r.getUser().getFullName())
                .userEmail(r.getUser().getEmail())
                .status(r.getStatus().name())
                .registeredAt(formatDt(r.getRegisteredAt()))
                .checkedIn(Boolean.TRUE.equals(r.getCheckedIn()))
                .checkedInAt(r.getCheckedInAt() != null ? formatDt(r.getCheckedInAt()) : null)
                .build();
    }

    private EventResponse toResponse(EventCommunity e, Long currentUserId) {
        boolean isRegistered = currentUserId != null && regRepo.existsByEventIdAndUserId(e.getId(), currentUserId);

        // Generate fresh S3/CloudFront URLs from stored media objects at read time
        java.util.Optional<MediaObject> imageMOpt = e.getImageMediaExternalId() != null
                ? mediaRepo.findByExternalIdAndDeletedFalse(e.getImageMediaExternalId())
                : java.util.Optional.empty();
        String imageUrl   = imageMOpt.map(m -> mediaUrlService.generateUrl(m)).orElse(e.getImageUrl());
        String imageMediaId = imageMOpt.map(m -> m.getExternalId().toString()).orElse(null);

        java.util.Optional<MediaObject> scannerMOpt = e.getScannerMediaExternalId() != null
                ? mediaRepo.findByExternalIdAndDeletedFalse(e.getScannerMediaExternalId())
                : java.util.Optional.empty();
        String scannerUrl   = scannerMOpt.map(m -> mediaUrlService.generateUrl(m)).orElse(e.getScannerUrl());
        String scannerMediaId = scannerMOpt.map(m -> m.getExternalId().toString()).orElse(null);

        List<TicketTypeDto> parsedTicketTypes = null;
        List<EventTicketCategory> savedCategories = ticketCategoryRepo.findByEventIdOrderByDisplayOrderAscIdAsc(e.getId());
        if (savedCategories != null && !savedCategories.isEmpty()) {
            parsedTicketTypes = savedCategories.stream().map(c -> TicketTypeDto.builder()
                    .id(c.getTicketCode() != null ? c.getTicketCode() : String.valueOf(c.getId()))
                    .name(c.getName())
                    .price(c.getPrice())
                    .qty(c.getCapacity() != null ? c.getCapacity() : c.getSeats())
                    .seats(c.getSeats() != null ? c.getSeats() : c.getCapacity())
                    .capacity(c.getCapacity())
                    .description(c.getDescription())
                    .build()).toList();
        } else if (e.getTicketTypesJson() != null && !e.getTicketTypesJson().isBlank()) {
            try {
                parsedTicketTypes = objectMapper.readValue(
                        e.getTicketTypesJson(),
                        new TypeReference<List<TicketTypeDto>>() {}
                );
            } catch (Exception ex) {
                log.warn("Failed to deserialize ticketTypesJson for event {}: {}", e.getId(), ex.getMessage());
            }
        }

        // Live dynamic active attendees count (direct + booking registrations)
        int liveAttendees = 0;
        if (e.getRegistrations() != null && !e.getRegistrations().isEmpty()) {
            liveAttendees += (int) e.getRegistrations().stream()
                    .filter(r -> r.getStatus() != EventRegistration.RegistrationStatus.CANCELLED)
                    .count();
        } else if (regRepo != null && e.getId() != null) {
            liveAttendees += (int) regRepo.findByEventId(e.getId()).stream()
                    .filter(r -> r.getStatus() != EventRegistration.RegistrationStatus.CANCELLED)
                    .count();
        }

        if (bookingRegRepo != null && e.getId() != null) {
            liveAttendees += (int) (bookingRegRepo.countByActivityIdAndStatusNot("event-" + e.getId(), "CANCELLED")
                    + bookingRegRepo.countByActivityIdAndStatusNot(String.valueOf(e.getId()), "CANCELLED"));
        }

        Integer effectiveCapacity = e.getCapacity() != null ? e.getCapacity() : (e.getMaxAttendees() != null ? e.getMaxAttendees() : 100);

        return EventResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .description(e.getDescription())
                .type(e.getType().name())
                .startDate(e.getStartDate().toString())
                .endDate(e.getEndDate() != null ? e.getEndDate().toString() : null)
                .startTime(e.getStartTime() != null ? e.getStartTime().toString() : null)
                .endTime(e.getEndTime() != null ? e.getEndTime().toString() : null)
                .locationType(e.getLocationType().name())
                .location(e.getLocation())
                .priceType(e.getPriceType().name())
                .price(e.getPrice())
                .capacity(effectiveCapacity)
                .imageUrl(imageUrl)
                .imageMediaId(imageMediaId)
                .scannerUrl(scannerUrl)
                .scannerMediaId(scannerMediaId)
                .organizerName(e.getOrganizerName())
                .organizerContact(e.getOrganizerContact())
                .venue(e.getVenue())
                .city(e.getCity())
                .category(e.getCategory())
                .status(e.getStatus() != null ? e.getStatus().name() : EventCommunity.EventStatus.PUBLISHED.name())
                .paymentModes(e.getPaymentModes())
                .upiId(e.getUpiId())
                .notes(e.getNotes())
                .contactsJson(e.getContactsJson())
                .paymentInstructions(e.getPaymentInstructions())
                .ticketTypesJson(e.getTicketTypesJson())
                .ticketTypes(parsedTicketTypes)
                .maxAttendees(effectiveCapacity)
                .registrationDeadline(e.getRegistrationDeadline() != null ? e.getRegistrationDeadline().toString() : null)
                .registrationCount(liveAttendees)
                .createdById(e.getCreatedBy() != null ? e.getCreatedBy().getId() : null)
                .createdByName(e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null)
                .communityId(e.getCommunity() != null ? e.getCommunity().getId() : null)
                .attendees(liveAttendees)
                .isRegistered(isRegistered)
                .createdAt(formatDt(e.getCreatedAt()))
                .build();
    }

    private void notifyRegisteredUsers(EventCommunity event, String title, String body) {
        try {
            List<EventRegistration> registrations = event.getRegistrations();
            if (registrations == null || registrations.isEmpty()) return;
            for (EventRegistration reg : registrations) {
                if (reg.getUser() == null) continue;
                Notification notification = Notification.builder()
                        .user(reg.getUser())
                        .community(event.getCommunity())
                        .type(NotificationType.EVENT_UPDATED)
                        .category(NotificationCategory.EVENTS)
                        .title(title)
                        .body(body)
                        .referenceType(ReferenceType.EVENT)
                        .referenceId(event.getId())
                        .build();
                notificationRepo.save(notification);
            }
        } catch (Exception ex) {
            log.warn("Failed to send event update notifications for event {}: {}", event.getId(), ex.getMessage());
        }
    }

    private UUID verifyAndParseMediaId(String mediaId, String context) {
        return verifyAndParseMediaId(mediaId, context, null);
    }

    private UUID verifyAndParseMediaId(String mediaId, String context, Community community) {
        if (mediaId == null || mediaId.isBlank()) return null;
        UUID uuid;
        try {
            uuid = UUID.fromString(mediaId);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("MediaObject", "id", mediaId);
        }
        MediaObject media = mediaRepo.findByExternalIdAndDeletedFalse(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("MediaObject", "id", uuid.toString()));
        if (community != null && media.getCommunityId() != null && !community.getId().equals(media.getCommunityId())) {
            throw new IllegalArgumentException("Media must belong to the same community");
        }
        return uuid;
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try { return Enum.valueOf(enumClass, value); }
        catch (IllegalArgumentException e) { return null; }
    }

    private <E extends Enum<E>> E parseEnumOrDefault(Class<E> enumClass, String value, E def) {
        E r = parseEnum(enumClass, value);
        return r != null ? r : def;
    }

    private LocalDate parseLocalDate(String str) {
        if (str == null || str.isBlank()) return null;
        String clean = str.contains("T") ? str.split("T")[0] : str;
        try {
            return LocalDate.parse(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime parseLocalTime(String str) {
        if (str == null || str.isBlank()) return null;
        String clean = str.contains("T") ? str.substring(str.indexOf("T") + 1) : str;
        if (clean.length() > 5) clean = clean.substring(0, 5);
        try {
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return null;
        }
    }

    private void validateTicketCategoriesCapacity(List<TicketTypeDto> ticketTypes, Integer maxLimit) {
        if (ticketTypes == null || ticketTypes.isEmpty() || maxLimit == null || maxLimit <= 0) {
            return;
        }
        int totalCategorySeats = 0;
        for (TicketTypeDto dto : ticketTypes) {
            if (dto.getName() == null || dto.getName().isBlank()) continue;
            Object seatsVal = dto.getQty() != null ? dto.getQty() : (dto.getSeats() != null ? dto.getSeats() : dto.getCapacity());
            if (seatsVal != null) {
                try {
                    int seats = Integer.parseInt(seatsVal.toString().replaceAll("[^0-9]", ""));
                    totalCategorySeats += seats;
                } catch (Exception ignored) {}
            }
        }
        if (totalCategorySeats > maxLimit) {
            throw new ManaCommunityException(
                    "Total seats across all ticket categories (" + totalCategorySeats + ") cannot exceed Event Max Capacity (" + maxLimit + ")",
                    HttpStatus.BAD_REQUEST,
                    "CATEGORY_CAPACITY_EXCEEDED"
            );
        }
    }

    private void saveTicketCategories(EventCommunity event, List<TicketTypeDto> ticketTypes) {
        if (ticketTypes == null || ticketTypes.isEmpty()) {
            return;
        }
        ticketCategoryRepo.deleteByEventId(event.getId());
        int order = 1;
        for (TicketTypeDto dto : ticketTypes) {
            if (dto.getName() == null || dto.getName().isBlank()) {
                continue;
            }
            Double price = 0.0;
            if (dto.getPrice() != null) {
                try {
                    price = Double.parseDouble(dto.getPrice().toString().replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}
            }
            Integer capacity = null;
            Object seatsVal = dto.getQty() != null ? dto.getQty() : (dto.getSeats() != null ? dto.getSeats() : dto.getCapacity());
            if (seatsVal != null) {
                try {
                    capacity = Integer.parseInt(seatsVal.toString().replaceAll("[^0-9]", ""));
                } catch (Exception ignored) {}
            }

            EventTicketCategory cat = EventTicketCategory.builder()
                    .event(event)
                    .communityId(event.getCommunity() != null ? event.getCommunity().getId() : null)
                    .ticketCode(dto.getId() != null ? dto.getId() : "t" + System.currentTimeMillis() + "_" + order)
                    .name(dto.getName().trim())
                    .price(price)
                    .capacity(capacity)
                    .seats(capacity)
                    .description(dto.getDescription())
                    .displayOrder(order++)
                    .isActive(true)
                    .build();
            ticketCategoryRepo.save(cat);
        }
    }
}
