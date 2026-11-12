package com.manacommunity.api.market.bazaar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.market.bazaar.dto.MarketBazaarDtos.*;
import com.manacommunity.api.market.bazaar.entity.*;
import com.manacommunity.api.market.bazaar.repository.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.payments.dto.CashfreeCreateOrderRequest;
import com.manacommunity.api.payments.dto.CashfreeOrderResponse;
import com.manacommunity.api.payments.entity.CashfreeOrder;
import com.manacommunity.api.payments.repository.CashfreeOrderRepository;
import com.manacommunity.api.payments.service.CashfreePaymentService;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.user.model.AppUser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MarketBazaarService {
    private static final List<String> ACTIVE_APPLICATIONS = List.of("SUBMITTED", "UNDER_REVIEW", "APPROVED", "PAYMENT_PENDING", "CONFIRMED");
    private final MarketEventRepository eventRepository;
    private final EventCommunityRepository communityEventRepository;
    private final MarketStallRepository stallRepository;
    private final MarketApplicationRepository applicationRepository;
    private final MarketBookingRepository bookingRepository;
    private final CashfreeOrderRepository cashfreeRepository;
    private final ObjectProvider<CashfreePaymentService> cashfreePaymentService;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public List<EventResponse> listEvents(AppUser user, String query, String city, String category, int page, int size) {
        Long communityId = communityId(user);
        return eventRepository.findPublished(communityId, List.of("PUBLISHED", "LIVE"))
                .stream()
                .filter(m -> blankOrContains(m.getEvent().getTitle(), query) || blankOrContains(m.getEvent().getDescription(), query))
                .filter(m -> List.of("PUBLISHED", "ACTIVE").contains(m.getEvent().getStatus().name()))
                .filter(m -> blankOrEquals(m.getEvent().getCity(), city))
                .filter(m -> blankOrEquals(m.getEvent().getCategory(), category))
                .skip((long) Math.max(page, 0) * Math.min(Math.max(size, 1), 100))
                .limit(Math.min(Math.max(size, 1), 100))
                .map(this::eventResponse).toList();
    }

    public List<EventResponse> listOrganizerEvents(AppUser user) {
        return eventRepository.findOrganizerEvents(communityId(user)).stream().map(this::eventResponse).toList();
    }

    /** Creates the platform event first, then enables Bazaar on that same event id. */
    @Transactional
    public EventResponse createEvent(AppUser user, CreateEventRequest request) {
        Community community = requireCommunity(user);
        if (request.endsAt() != null && request.endsAt().isBefore(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event end must be after its start.");
        }
        LocalDateTime start = request.startsAt();
        EventCommunity event = EventCommunity.builder()
                .title(request.title().trim()).description(request.description())
                .type(EventCommunity.EventType.FESTIVAL)
                .startDate(start.toLocalDate()).startTime(start.toLocalTime())
                .endDate(request.endsAt() == null ? null : request.endsAt().toLocalDate())
                .endTime(request.endsAt() == null ? null : request.endsAt().toLocalTime())
                .locationType(EventCommunity.LocationType.IN_PERSON).location(request.venue().trim())
                .venue(request.venue().trim()).city(request.city() == null ? community.getCity() : request.city().trim())
                .category(request.category() == null ? "MARKET" : request.category())
                .status(EventCommunity.EventStatus.PUBLISHED).organizerName(user.getFullName())
                .organizerContact(user.getPhone()).createdBy(user).community(community).build();
        EventCommunity savedEvent = communityEventRepository.save(event);
        CommunityMarketEvent bazaar = CommunityMarketEvent.builder().event(savedEvent).status("PUBLISHED")
                .bannerUrl(request.bannerUrl()).build();
        CommunityMarketEvent savedBazaar = eventRepository.save(bazaar);
        auditService.record(AuditAction.MARKET_EVENT_CREATED, AuditModule.EVENTS, "MarketEvent", String.valueOf(savedBazaar.getId()));
        return eventResponse(savedBazaar);
    }

    public List<StallResponse> listStalls(AppUser user, Long eventId) {
        requireEvent(eventId, communityId(user));
        return stallRepository.findByEventIdOrderByStallNumberAsc(eventId).stream().map(this::stallResponse).toList();
    }

    @Transactional
    public StallResponse createStall(AppUser user, Long eventId, CreateStallRequest request) {
        CommunityMarketEvent event = requireEvent(eventId, communityId(user));
        MarketStall stall = MarketStall.builder().event(event).stallNumber(request.stallNumber().trim())
                .zone(request.zone()).packageName(request.packageName().trim()).description(request.description())
                .size(request.size()).price(request.price()).currency(request.currency() == null ? "INR" : request.currency().toUpperCase())
                .facilities(writeJson(request.facilities())).allowedCategories(writeJson(request.allowedCategories()))
                .floorRow(request.floorRow()).floorColumn(request.floorColumn())
                .status("AVAILABLE").build();
        MarketStall saved = stallRepository.save(stall);
        auditService.record(AuditAction.MARKET_STALL_CREATED, AuditModule.MARKETPLACE, "MarketStall", String.valueOf(saved.getId()));
        return stallResponse(saved);
    }

    @Transactional
    public ApplicationResponse apply(AppUser user, Long eventId, ApplyRequest request) {
        CommunityMarketEvent event = requireEvent(eventId, communityId(user));
        MarketStall stall = stallRepository.findByIdAndEventIdForUpdate(request.stallId(), eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Market stall", request.stallId()));
        if (!"AVAILABLE".equals(stall.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This stall is no longer available.");
        if (applicationRepository.existsByEventIdAndApplicantIdAndStallIdAndStatusIn(eventId, user.getId(), stall.getId(), ACTIVE_APPLICATIONS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active application for this stall.");
        }
        MarketApplication application = MarketApplication.builder().event(event).stall(stall).applicant(user)
                .vendorName(request.vendorName().trim()).businessName(request.businessName().trim())
                .category(request.category().trim()).phone(request.phone().trim()).products(request.products().trim())
                .complianceDocuments(writeJson(request.complianceDocuments())).status("SUBMITTED").build();
        MarketApplication saved = applicationRepository.save(application);
        auditService.record(AuditAction.MARKET_APPLICATION_SUBMITTED, AuditModule.MARKETPLACE, "MarketApplication", String.valueOf(saved.getId()));
        return applicationResponse(saved);
    }

    public List<ApplicationResponse> listApplications(AppUser user, Long eventId) {
        requireEvent(eventId, communityId(user));
        return applicationRepository.findByEventIdOrderByRequestedAtDesc(eventId).stream().map(this::applicationResponse).toList();
    }

    public List<ApplicationResponse> listMyApplications(AppUser user) {
        return applicationRepository.findByApplicantIdOrderByRequestedAtDesc(user.getId()).stream().map(this::applicationResponse).toList();
    }

    @Transactional
    public ApplicationResponse approve(AppUser user, Long applicationId, Long stallId) {
        MarketApplication application = applicationRepository.findScoped(applicationId, communityId(user))
                .orElseThrow(() -> new ResourceNotFoundException("Market application", applicationId));
        if (!"SUBMITTED".equals(application.getStatus()) && !"UNDER_REVIEW".equals(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not awaiting review.");
        }
        MarketStall stall = stallRepository.findByIdAndEventIdForUpdate(stallId, application.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Market stall", stallId));
        if (!"AVAILABLE".equals(stall.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This stall is no longer available.");
        application.setStall(stall);
        application.setStatus("PAYMENT_PENDING");
        stall.setStatus("HELD");
        MarketBooking booking = MarketBooking.builder().application(application).stall(stall).applicant(application.getApplicant())
                .amount(stall.getPrice()).currency(stall.getCurrency()).status("PAYMENT_PENDING")
                .holdExpiresAt(LocalDateTime.now().plusMinutes(15)).build();
        bookingRepository.save(booking);
        MarketApplication saved = applicationRepository.save(application);
        auditService.record(AuditAction.MARKET_APPLICATION_APPROVED, AuditModule.MARKETPLACE, "MarketApplication", String.valueOf(saved.getId()), "SUBMITTED", "PAYMENT_PENDING");
        return applicationResponse(saved);
    }

    @Transactional
    public ApplicationResponse reject(AppUser user, Long applicationId, String reason) {
        MarketApplication application = applicationRepository.findScoped(applicationId, communityId(user))
                .orElseThrow(() -> new ResourceNotFoundException("Market application", applicationId));
        if (!List.of("SUBMITTED", "UNDER_REVIEW").contains(application.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Application is not awaiting review.");
        }
        application.setStatus("REJECTED");
        application.setRejectionReason(reason.trim());
        MarketApplication saved = applicationRepository.save(application);
        auditService.record(AuditAction.MARKET_APPLICATION_REJECTED, AuditModule.MARKETPLACE, "MarketApplication", String.valueOf(saved.getId()), "SUBMITTED", "REJECTED");
        return applicationResponse(saved);
    }

    @Transactional
    public CashfreeOrderResponse checkout(AppUser user, Long bookingId) {
        MarketBooking booking = bookingRepository.findByIdAndApplicantIdForUpdate(bookingId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Market booking", bookingId));
        if (user.getCommunity() == null || !user.getCommunity().getId().equals(booking.getApplication().getEvent().getEvent().getCommunity().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Market booking not found.");
        }
        if (!"PAYMENT_PENDING".equals(booking.getStatus()) || booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The payment hold has expired.");
        }
        if (booking.getPaymentOrderId() != null) {
            CashfreeOrder existing = cashfreeRepository.findByManaOrderId(booking.getPaymentOrderId()).orElse(null);
            if (existing != null && List.of("CREATED", "PENDING").contains(existing.getStatus())) {
                CashfreePaymentService payments = cashfreePaymentService.getIfAvailable();
                if (payments == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payments are not enabled.");
                return payments.getUserOrder(existing.getManaOrderId(), user);
            }
        }
        CashfreeCreateOrderRequest request = new CashfreeCreateOrderRequest();
        request.setAmount(booking.getAmount());
        request.setReferenceType("MARKET_STALL_BOOKING");
        request.setReferenceId(booking.getId());
        request.setDescription("Market stall " + booking.getStall().getStallNumber());
        CashfreePaymentService payments = cashfreePaymentService.getIfAvailable();
        if (payments == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payments are not enabled.");
        CashfreeOrderResponse response = payments.createOrder(request, user, user.getCommunity());
        booking.setPaymentOrderId(response.getManaOrderId());
        bookingRepository.save(booking);
        return response;
    }

    public EventAnalyticsResponse analytics(AppUser user, Long eventId) {
        requireEvent(eventId, communityId(user));
        return new EventAnalyticsResponse(
                stallRepository.countByEventIdAndStatus(eventId, "AVAILABLE") + stallRepository.countByEventIdAndStatus(eventId, "HELD") + stallRepository.countByEventIdAndStatus(eventId, "BOOKED"),
                stallRepository.countByEventIdAndStatus(eventId, "AVAILABLE"),
                stallRepository.countByEventIdAndStatus(eventId, "HELD"),
                stallRepository.countByEventIdAndStatus(eventId, "BOOKED"),
                applicationRepository.countByEventIdAndStatusIn(eventId, List.of("SUBMITTED", "UNDER_REVIEW")),
                bookingRepository.countByApplicationEventIdAndStatus(eventId, "CONFIRMED"),
                bookingRepository.sumConfirmedByEvent(eventId));
    }

    @Transactional
    public void expireHolds() {
        LocalDateTime now = LocalDateTime.now();
        for (MarketBooking booking : bookingRepository.findByStatusAndHoldExpiresAtBefore("PAYMENT_PENDING", now)) {
            booking.setStatus("EXPIRED");
            booking.getApplication().setStatus("EXPIRED");
            booking.getStall().setStatus("AVAILABLE");
        }
    }

    @Transactional
    public void confirmPaidBooking(Long bookingId, Long payerId, BigDecimal paidAmount) {
        MarketBooking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) return;
        if (!booking.getApplicant().getId().equals(payerId) || booking.getAmount().compareTo(paidAmount) != 0
                || !"PAYMENT_PENDING".equals(booking.getStatus()) || booking.getHoldExpiresAt().isBefore(LocalDateTime.now())) {
            booking.setStatus("REFUND_PENDING");
            booking.getApplication().setStatus("REFUND_PENDING");
            return;
        }
        booking.setStatus("CONFIRMED");
        booking.getApplication().setStatus("CONFIRMED");
        booking.getStall().setStatus("BOOKED");
        auditService.record(AuditAction.MARKET_BOOKING_CONFIRMED, AuditModule.MARKETPLACE, "MarketBooking", String.valueOf(booking.getId()), "PAYMENT_PENDING", "CONFIRMED");
    }

    private EventResponse eventResponse(CommunityMarketEvent market) {
        EventCommunity event = market.getEvent();
        LocalDateTime startsAt = LocalDateTime.of(event.getStartDate(), event.getStartTime() == null ? LocalTime.MIDNIGHT : event.getStartTime());
        LocalDateTime endsAt = event.getEndDate() == null ? null : LocalDateTime.of(event.getEndDate(), event.getEndTime() == null ? LocalTime.MIDNIGHT : event.getEndTime());
        return new EventResponse(market.getId(), event.getTitle(), event.getDescription(), event.getCommunity().getId(),
                event.getCommunity().getName(), event.getCity(), event.getVenue() == null ? event.getLocation() : event.getVenue(),
                startsAt, endsAt, market.getStatus(), event.getCategory(), market.getBannerUrl(),
                bookingRepository.countByApplicationEventIdAndStatus(market.getId(), "CONFIRMED"),
                stallRepository.countByEventIdAndStatus(market.getId(), "AVAILABLE"));
    }
    private StallResponse stallResponse(MarketStall stall) {
        return new StallResponse(stall.getId(), stall.getEvent().getId(), stall.getStallNumber(), stall.getZone(), null,
                stall.getPackageName(), stall.getDescription(), stall.getSize(), stall.getPrice(), stall.getCurrency(),
                readJson(stall.getFacilities()), readJson(stall.getAllowedCategories()), stall.getStatus(),
                stall.getFloorRow(), stall.getFloorColumn());
    }
    private ApplicationResponse applicationResponse(MarketApplication application) {
        MarketBooking booking = bookingRepository.findByApplicationId(application.getId()).orElse(null);
        return new ApplicationResponse(application.getId(), application.getEvent().getId(), application.getEvent().getEvent().getTitle(),
                application.getStall().getId(), application.getStall().getStallNumber(), application.getVendorName(),
                application.getBusinessName(), application.getCategory(), application.getPhone(), application.getProducts(),
                readJson(application.getComplianceDocuments()), application.getStatus(), application.getRequestedAt(),
                booking == null ? null : booking.getAmount(), booking == null ? "INR" : booking.getCurrency(),
                booking == null ? null : booking.getId(), booking == null ? null : booking.getPaymentOrderId(),
                application.getRejectionReason());
    }
    private CommunityMarketEvent requireEvent(Long id, Long communityId) {
        return eventRepository.findScoped(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Market event", id));
    }
    private Community requireCommunity(AppUser user) {
        if (user.getCommunity() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Join a community before using Market Bazaar.");
        return user.getCommunity();
    }
    private Long communityId(AppUser user) { return requireCommunity(user).getId(); }
    private boolean blankOrContains(String value, String query) { return query == null || query.isBlank() || (value != null && value.toLowerCase().contains(query.trim().toLowerCase())); }
    private boolean blankOrEquals(String value, String filter) { return filter == null || filter.isBlank() || (value != null && value.equalsIgnoreCase(filter.trim())); }
    private String writeJson(List<String> values) {
        try { return objectMapper.writeValueAsString(values == null ? List.of() : values); }
        catch (Exception e) { throw new IllegalStateException("Could not encode Bazaar list value.", e); }
    }
    private List<String> readJson(String json) {
        try { return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }
}





