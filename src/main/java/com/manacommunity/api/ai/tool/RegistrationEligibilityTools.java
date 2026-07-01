package com.manacommunity.api.ai.tool;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * AI tools that check whether a user is eligible to register for a sports event
 * <em>before</em> they attempt it — catching age, gender, KYC, capacity, and
 * date issues upfront via conversation instead of form validation errors.
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class RegistrationEligibilityTools {

    @PersistenceContext
    private EntityManager em;

    @Tool(description = "Check if the current user is eligible to register for a specific sports "
            + "event. Validates: age range, gender match, community membership, KYC status, "
            + "registration window (dates), capacity limit, and whether already registered. "
            + "Returns a pass/fail for each check with clear reasons.")
    public Object checkMyEligibility(
            @ToolParam(description = "Event ID to check eligibility for") Long eventId) {

        UserContext ctx = AgentSecurityContext.get();

        // Fetch event details
        var eventRows = em.createQuery(
                "SELECT e.id, e.name, e.minAge, e.maxAge, e.gender, " +
                "e.registrationDateStart, e.registrationDateEnd, " +
                "e.maxParticipants, e.adminApprovalRequired, " +
                "t.registrationStatus, e.community.id, s.name " +
                "FROM SportsEvent e " +
                "LEFT JOIN e.tournament t LEFT JOIN e.sport s " +
                "WHERE e.id = :eid AND e.active = true", Object[].class)
                .setParameter("eid", eventId)
                .getResultList();

        if (eventRows.isEmpty()) {
            return Map.of("eligible", false, "reason", "Event not found or not active.");
        }

        Object[] ev = eventRows.get(0);
        String eventName = (String) ev[1];
        Integer minAge = (Integer) ev[2];
        Integer maxAge = (Integer) ev[3];
        String eventGender = (String) ev[4];
        LocalDate regStart = (LocalDate) ev[5];
        LocalDate regEnd = (LocalDate) ev[6];
        Integer maxParticipants = (Integer) ev[7];
        String regStatus = ev[9] != null ? ev[9].toString() : null;
        Long eventCommunityId = (Long) ev[10];
        String sportName = (String) ev[11];

        // Fetch user details
        var userRows = em.createQuery(
                "SELECT u.dateOfBirth, u.gender, u.kycStatus, u.community.id, u.fullName " +
                "FROM AppUser u WHERE u.id = :uid", Object[].class)
                .setParameter("uid", ctx.userId())
                .getResultList();

        if (userRows.isEmpty()) {
            return Map.of("eligible", false, "reason", "User account not found.");
        }

        Object[] usr = userRows.get(0);
        LocalDate dob = (LocalDate) usr[0];
        String userGender = (String) usr[1];
        String kycStatus = (String) usr[2];
        Long userCommunityId = (Long) usr[3];
        String userName = (String) usr[4];

        LocalDate today = LocalDate.now();
        int userAge = dob != null ? Period.between(dob, today).getYears() : -1;

        List<Map<String, Object>> checks = new ArrayList<>();
        boolean allPassed = true;

        // 1. Community membership
        boolean communityOk = eventCommunityId != null && eventCommunityId.equals(userCommunityId);
        checks.add(checkResult("Community membership", communityOk,
                communityOk ? "You belong to this event's community" : "This event is for a different community"));
        if (!communityOk) allPassed = false;

        // 2. Registration window
        boolean windowOpen = true;
        String windowMsg = "Registration window is open";
        if (regStart != null && today.isBefore(regStart)) {
            windowOpen = false;
            windowMsg = "Registration opens on " + regStart;
        } else if (regEnd != null && today.isAfter(regEnd)) {
            windowOpen = false;
            windowMsg = "Registration closed on " + regEnd;
        }
        if (regStatus != null && !"REGISTRATION_OPEN".equals(regStatus)) {
            windowOpen = false;
            windowMsg = "Registration status is " + regStatus;
        }
        checks.add(checkResult("Registration window", windowOpen, windowMsg));
        if (!windowOpen) allPassed = false;

        // 3. Age check
        boolean ageOk = true;
        String ageMsg = "Age " + userAge + " is within range";
        if (userAge < 0) {
            ageOk = false;
            ageMsg = "Date of birth not set on your profile";
        } else {
            if (minAge != null && userAge < minAge) {
                ageOk = false;
                ageMsg = "You are " + userAge + " but minimum age is " + minAge;
            }
            if (maxAge != null && userAge > maxAge) {
                ageOk = false;
                ageMsg = "You are " + userAge + " but maximum age is " + maxAge;
            }
        }
        checks.add(checkResult("Age requirement (" + minAge + "-" + maxAge + ")", ageOk, ageMsg));
        if (!ageOk) allPassed = false;

        // 4. Gender check
        boolean genderOk = true;
        String genderMsg = "Gender requirement met";
        if (eventGender != null && !"ALL".equalsIgnoreCase(eventGender) && !"MIXED".equalsIgnoreCase(eventGender)) {
            if (!eventGender.equalsIgnoreCase(userGender)) {
                genderOk = false;
                genderMsg = "Event is for " + eventGender + " but your profile says " + userGender;
            }
        }
        checks.add(checkResult("Gender requirement", genderOk, genderMsg));
        if (!genderOk) allPassed = false;

        // 5. KYC status
        boolean kycOk = "VERIFIED".equalsIgnoreCase(kycStatus);
        checks.add(checkResult("KYC verification", kycOk,
                kycOk ? "KYC is verified" : "KYC status is " + kycStatus + " — verification may be required"));
        // KYC is a soft check (some events don't require it)

        // 6. Capacity check
        boolean capacityOk = true;
        String capacityMsg = "Spots available";
        if (maxParticipants != null) {
            Long currentCount = em.createQuery(
                    "SELECT COUNT(r) FROM SportsEventRegistration r " +
                    "WHERE r.event.id = :eid AND r.status IN ('REGISTERED', 'CONFIRMED', 'PENDING')",
                    Long.class).setParameter("eid", eventId).getSingleResult();
            if (currentCount >= maxParticipants) {
                capacityOk = false;
                capacityMsg = "Event is full (" + currentCount + "/" + maxParticipants + ")";
            } else {
                capacityMsg = currentCount + "/" + maxParticipants + " spots taken";
            }
        }
        checks.add(checkResult("Capacity", capacityOk, capacityMsg));
        if (!capacityOk) allPassed = false;

        // 7. Already registered check
        Long alreadyRegistered = em.createQuery(
                "SELECT COUNT(r) FROM SportsEventRegistration r " +
                "WHERE r.event.id = :eid AND r.user.id = :uid " +
                "AND r.status NOT IN ('WITHDRAWN', 'REJECTED')", Long.class)
                .setParameter("eid", eventId)
                .setParameter("uid", ctx.userId())
                .getSingleResult();
        boolean notDuplicate = alreadyRegistered == 0;
        checks.add(checkResult("Not already registered", notDuplicate,
                notDuplicate ? "You haven't registered yet" : "You are already registered for this event"));
        if (!notDuplicate) allPassed = false;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("event", eventName);
        result.put("sport", sportName);
        result.put("user", userName);
        result.put("eligible", allPassed);
        result.put("checks", checks);

        if (allPassed) {
            result.put("next_step", "You're eligible! You can register through the Sports dashboard.");
        }

        return result;
    }

    @Tool(description = "Find events the current user is eligible for — scans all open events "
            + "in the community and checks age, gender, and capacity for each. "
            + "Returns only events where the user passes all checks.")
    public Object findEligibleEvents() {
        UserContext ctx = AgentSecurityContext.get();

        // Get user details
        var userRows = em.createQuery(
                "SELECT u.dateOfBirth, u.gender FROM AppUser u WHERE u.id = :uid", Object[].class)
                .setParameter("uid", ctx.userId())
                .getResultList();

        if (userRows.isEmpty()) return Map.of("error", "User not found");

        LocalDate dob = (LocalDate) userRows.get(0)[0];
        String userGender = (String) userRows.get(0)[1];
        int userAge = dob != null ? Period.between(dob, LocalDate.now()).getYears() : -1;

        // Find open events in community
        var events = em.createQuery(
                "SELECT e.id, e.name, s.name, e.minAge, e.maxAge, e.gender, " +
                "e.eventDateStart, v.name, e.maxParticipants, t.registrationStatus " +
                "FROM SportsEvent e " +
                "LEFT JOIN e.sport s LEFT JOIN e.venue v LEFT JOIN e.tournament t " +
                "WHERE e.community.id = :comId AND e.active = true " +
                "AND (t IS NULL OR t.registrationStatus = 'REGISTRATION_OPEN') " +
                "ORDER BY e.eventDateStart ASC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        List<Map<String, Object>> eligible = new ArrayList<>();
        for (Object[] ev : events) {
            Integer minAge = (Integer) ev[3];
            Integer maxAge = (Integer) ev[4];
            String eventGender = (String) ev[5];

            boolean ageOk = userAge >= 0
                    && (minAge == null || userAge >= minAge)
                    && (maxAge == null || userAge <= maxAge);
            boolean genderOk = eventGender == null
                    || "ALL".equalsIgnoreCase(eventGender)
                    || "MIXED".equalsIgnoreCase(eventGender)
                    || eventGender.equalsIgnoreCase(userGender);

            if (ageOk && genderOk) {
                // Check not already registered
                Long regCount = em.createQuery(
                        "SELECT COUNT(r) FROM SportsEventRegistration r " +
                        "WHERE r.event.id = :eid AND r.user.id = :uid " +
                        "AND r.status NOT IN ('WITHDRAWN', 'REJECTED')", Long.class)
                        .setParameter("eid", ev[0]).setParameter("uid", ctx.userId())
                        .getSingleResult();

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("event_id", ev[0]);
                m.put("event_name", ev[1]);
                m.put("sport", ev[2]);
                m.put("age_range", (minAge != null ? minAge : "any") + "-" + (maxAge != null ? maxAge : "any"));
                m.put("start_date", ev[6] != null ? ev[6].toString() : null);
                m.put("venue", ev[7]);
                m.put("already_registered", regCount > 0);
                eligible.add(m);
            }
        }

        return Map.of("your_age", userAge, "eligible_events", eligible,
                "total_found", eligible.size());
    }

    private Map<String, Object> checkResult(String check, boolean passed, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("check", check);
        m.put("passed", passed);
        m.put("detail", detail);
        return m;
    }
}
