package com.manacommunity.api.events.dto;

import com.manacommunity.api.events.entity.EventPoojaUserRegistration;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for the admin-only POST /api/events/pooja-registrations/admin-create endpoint.
 * Bundles the target user identity, the override reason, and all registration fields in one object
 * so nothing leaks into query parameters.
 */
@Data
public class AdminPoojaRegistrationRequest {

    /** ID of the member being registered. Required — admin always acts on behalf of someone. */
    @NotNull(message = "targetUserId is required for admin registration")
    private Long targetUserId;

    /** Why the admin is creating this registration (may bypass capacity / duplicate checks). */
    private String overrideReason;

    // ── Registration fields ───────────────────────────────────────────────────

    private Long eventId;
    private Long scheduleId;
    private Long reservationId;
    private Long poojaSevaTimeSlotsId;

    private String poojaSlotName;
    private String poojaSlotDate;
    private String poojaSlotTime;

    private String participantName;
    private String gotram;
    private String phone;
    private String email;
    private String flatNo;

    private Integer devoteeCount;
    private String attendingDevotees;

    private String venue;
    private String mandap;
    private String panditName;

    private Double bookingFee;
    private String paymentStatus;
    private String paymentMethod;
    private String prasadamMode;
    private String status;
    private String notes;

    /** Converts this DTO to the entity used by the service layer. */
    public EventPoojaUserRegistration toRegistration() {
        EventPoojaUserRegistration r = new EventPoojaUserRegistration();
        r.setEventId(eventId);
        r.setScheduleId(scheduleId);
        r.setReservationId(reservationId);
        r.setPoojaSevaTimeSlotsId(poojaSevaTimeSlotsId);
        r.setPoojaSlotName(poojaSlotName);
        r.setPoojaSlotDate(poojaSlotDate);
        r.setPoojaSlotTime(poojaSlotTime);
        r.setParticipantName(participantName != null ? participantName : "");
        r.setGotram(gotram);
        r.setPhone(phone);
        r.setEmail(email);
        r.setFlatNo(flatNo);
        r.setDevoteeCount(devoteeCount);
        r.setAttendingDevotees(attendingDevotees);
        r.setVenue(venue);
        r.setMandap(mandap);
        r.setPanditName(panditName);
        r.setBookingFee(bookingFee);
        r.setPaymentStatus(paymentStatus);
        r.setPaymentMethod(paymentMethod);
        r.setPrasadamMode(prasadamMode);
        r.setStatus(status);
        r.setNotes(notes);
        r.setOverrideReason(overrideReason);
        return r;
    }
}
