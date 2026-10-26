package com.manacommunity.api.sports.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.email.RegistrationEmailService;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.AuditService;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsRegistrationImportProcessor {

    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final SportsPlayerCategoryRepository categoryRepo;
    private final SportsEventFormatRepository formatRepo;
    private final AppUserRepository userRepo;
    private final SportsExternalResponseRepository responseRepo;
    private final RegistrationEmailService registrationEmailService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SportsExternalResponse processResponse(
            SportsExternalForm form,
            String externalResponseId,
            String respondentEmail,
            LocalDateTime submittedAt,
            Map<String, Object> answersMap
    ) {
        // 1. Idempotency Check: Don't process existing response ID twice
        Optional<SportsExternalResponse> existing = responseRepo.findByFormIdAndExternalResponseId(form.getId(), externalResponseId);
        if (existing.isPresent()) {
            log.info("Response formId={} externalResponseId={} already processed with status={}",
                    form.getId(), externalResponseId, existing.get().getStatus());
            return existing.get();
        }

        String rawPayloadJson = "";
        try {
            rawPayloadJson = objectMapper.writeValueAsString(answersMap);
        } catch (Exception e) {
            rawPayloadJson = "{}";
        }

        SportsExternalResponse staging = SportsExternalResponse.builder()
                .form(form)
                .externalResponseId(externalResponseId)
                .respondentEmail(respondentEmail)
                .submittedAt(submittedAt != null ? submittedAt : LocalDateTime.now())
                .rawPayloadJson(rawPayloadJson)
                .status(SportsExternalResponse.ResponseStatus.PROCESSING)
                .build();
        staging = responseRepo.save(staging);

        SportsEvent event = form.getEvent();
        if (event == null) {
            staging.setStatus(SportsExternalResponse.ResponseStatus.REJECTED);
            staging.setValidationErrors("Linked sports event is missing or deleted");
            return responseRepo.save(staging);
        }

        // 2. Resolve Canonical Fields using Field Mappings
        Map<String, String> canonical = extractCanonicalFields(form.getFieldMappings(), answersMap);

        String playerName = canonical.getOrDefault("playerName", "").trim();
        String email = canonical.getOrDefault("email", respondentEmail != null ? respondentEmail : "").trim();
        String phone = canonical.getOrDefault("phone", "").trim();
        String flatNumber = canonical.getOrDefault("flatNumber", "").trim();
        String genderRaw = canonical.getOrDefault("gender", "").trim();
        String ageRaw = canonical.getOrDefault("age", "").trim();
        String categoryRaw = canonical.getOrDefault("categoryId", canonical.getOrDefault("categoryName", "")).trim();
        String formatRaw = canonical.getOrDefault("matchFormat", "").trim();
        String partnerName = canonical.getOrDefault("partnerName", "").trim();
        String partnerPhone = canonical.getOrDefault("partnerPhone", "").trim();
        String cricHeroesUrl = canonical.getOrDefault("cricheroesUrl", "").trim();

        List<String> validationErrors = new ArrayList<>();

        if (playerName.isEmpty()) {
            validationErrors.add("Player name is required");
        }

        // 3. Check Event Registration Status
        if (event.getStatus() != SportsEventStatus.REGISTRATION_OPEN) {
            staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
            staging.setReviewReason("EVENT_NOT_OPEN");
            staging.setValidationErrors("Event registration status is " + event.getStatus());
            return responseRepo.save(staging);
        }

        // 4. Check Event Capacity
        Integer maxParticipants = event.getMaxParticipants() != null
                ? event.getMaxParticipants()
                : (event.getTournament() != null ? event.getTournament().getMaxParticipants() : null);
        long currentCount = regRepo.countByEventId(event.getId());
        if (maxParticipants != null && currentCount >= maxParticipants) {
            staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
            staging.setReviewReason("EVENT_CAPACITY_FULL");
            staging.setValidationErrors("Tournament event has reached maximum capacity (" + maxParticipants + ")");
            return responseRepo.save(staging);
        }

        // 5. Resolve Age
        Integer age = null;
        if (!ageRaw.isEmpty()) {
            try {
                age = Integer.parseInt(ageRaw);
            } catch (NumberFormatException e) {
                // Try parsing ISO date
                try {
                    LocalDate dob = LocalDate.parse(ageRaw);
                    age = Period.between(dob, LocalDate.now()).getYears();
                } catch (Exception ignored) {
                    validationErrors.add("Unable to parse age or date of birth: " + ageRaw);
                }
            }
        }

        // 6. Resolve Gender
        String gender = normalizeGender(genderRaw);

        // 7. Resolve Category
        SportsPlayerCategory category = null;
        if (!categoryRaw.isEmpty()) {
            try {
                Long catId = Long.parseLong(categoryRaw);
                category = categoryRepo.findById(catId).orElse(null);
            } catch (NumberFormatException ignored) {
                category = categoryRepo.findByNameIgnoreCase(categoryRaw).orElse(null);
            }
        }
        if (category == null && event.getCategories() != null && event.getCategories().size() == 1) {
            // Default to single category if event only has 1
            category = event.getCategories().iterator().next();
        }

        if (category == null) {
            staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
            staging.setReviewReason("UNRESOLVED_CATEGORY");
            staging.setValidationErrors("Category '" + categoryRaw + "' could not be matched to an event category");
            return responseRepo.save(staging);
        }

        // Check if category is valid for event
        if (event.getCategories() != null && !event.getCategories().isEmpty()) {
            final Long catId = category.getId();
            boolean belongsToEvent = event.getCategories().stream().anyMatch(c -> c.getId().equals(catId));
            if (!belongsToEvent) {
                staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
                staging.setReviewReason("INELIGIBLE_CATEGORY");
                staging.setValidationErrors("Category '" + category.getName() + "' is not configured for event '" + event.getName() + "'");
                return responseRepo.save(staging);
            }
        }

        // 8. Validate Age & Gender Eligibility
        if (age != null) {
            int minAge = category.getMinAge() != null ? category.getMinAge() : (event.getMinAge() != null ? event.getMinAge() : 0);
            int maxAge = category.getMaxAge() != null ? category.getMaxAge() : (event.getMaxAge() != null ? event.getMaxAge() : 100);
            if (age < minAge || age > maxAge) {
                staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
                staging.setReviewReason("AGE_INELIGIBLE");
                staging.setValidationErrors("Age " + age + " is outside permitted range (" + minAge + "–" + maxAge + " yrs) for " + category.getName());
                return responseRepo.save(staging);
            }
        }

        if (category.getGender() != null && !category.getGender().isBlank() && !gender.isEmpty()) {
            String catG = category.getGender().trim().toUpperCase();
            if (!"ALL".equals(catG) && !"OPEN".equals(catG) && !"MIXED".equals(catG) && !"ANY".equals(catG)) {
                if (!gender.equalsIgnoreCase(catG)) {
                    staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
                    staging.setReviewReason("GENDER_MISMATCH");
                    staging.setValidationErrors("Category " + category.getName() + " requires gender " + catG + ", got " + gender);
                    return responseRepo.save(staging);
                }
            }
        }

        // 9. Resolve Match Format
        SportsEvent.MatchFormat matchFormat = resolveMatchFormat(formatRaw, category);
        SportsEventFormat eventFormat = null;
        if (matchFormat != null) {
            final SportsEvent.MatchFormat f = matchFormat;
            if (event.getEventFormats() != null) {
                eventFormat = event.getEventFormats().stream()
                        .filter(ef -> ef.getFormat() == f)
                        .findFirst()
                        .orElse(null);
            }
            if (eventFormat == null) {
                eventFormat = formatRepo.findByEventIdAndFormat(event.getId(), f).orElse(null);
            }
        }

        // 10. Check Duplicate Registration
        boolean isDuplicate = regRepo.existsDuplicateRegistration(
                event.getId(),
                playerName,
                email.isBlank() ? "no-email" : email,
                flatNumber.isBlank() ? "no-flat" : flatNumber,
                matchFormat
        );
        if (isDuplicate) {
            staging.setStatus(SportsExternalResponse.ResponseStatus.NEEDS_REVIEW);
            staging.setReviewReason("DUPLICATE_REGISTRATION");
            staging.setValidationErrors("Participant '" + playerName + "' is already registered in format " + matchFormat);
            return responseRepo.save(staging);
        }

        if (!validationErrors.isEmpty()) {
            staging.setStatus(SportsExternalResponse.ResponseStatus.REJECTED);
            staging.setValidationErrors(String.join("; ", validationErrors));
            return responseRepo.save(staging);
        }

        // 11. Match User or Register as Guest
        AppUser resolvedUser = null;
        if (!email.isBlank()) {
            resolvedUser = userRepo.findByEmail(email).orElse(null);
        }
        if (resolvedUser == null && !phone.isBlank()) {
            resolvedUser = userRepo.findByPhone(phone).orElse(null);
        }

        boolean isGuest = (resolvedUser == null);
        if (resolvedUser != null) {
            staging.setResolvedUser(resolvedUser);
        }

        // 12. Determine Registration Approval Status
        boolean requiresApproval = false;
        if (form.getApprovalMode() == SportsExternalForm.ApprovalMode.MANUAL) {
            requiresApproval = true;
        } else if (form.getApprovalMode() == SportsExternalForm.ApprovalMode.AUTO) {
            requiresApproval = false;
        } else {
            requiresApproval = Boolean.TRUE.equals(event.getAdminApprovalRequired());
        }

        SportsEventRegistration.RegistrationStatus initialStatus = requiresApproval
                ? SportsEventRegistration.RegistrationStatus.PENDING
                : SportsEventRegistration.RegistrationStatus.CONFIRMED;

        SportsEventRegistration.RegistrationSource regSource =
                form.getProvider() == SportsExternalForm.FormProvider.GOOGLE_FORMS
                        ? SportsEventRegistration.RegistrationSource.GOOGLE_FORMS
                        : SportsEventRegistration.RegistrationSource.EXTERNAL_FORM;

        // 13. Create & Persist SportsEventRegistration
        SportsEventRegistration reg = SportsEventRegistration.builder()
                .event(event)
                .community(form.getCommunity())
                .user(resolvedUser)
                .playerName(playerName)
                .email(email)
                .phone(phone)
                .flatNumber(flatNumber)
                .age(age)
                .category(category)
                .matchType(matchFormat)
                .eventFormat(eventFormat)
                .registrationSource(regSource)
                .externalResponseId(externalResponseId)
                .externalFormId(form.getId())
                .isExternalGuest(isGuest)
                .status(initialStatus)
                .cricheroesUrl(cricHeroesUrl.isBlank() ? null : cricHeroesUrl)
                .build();

        SportsEventRegistration savedReg = regRepo.save(reg);

        // 14. Update Staging Response to IMPORTED
        staging.setStatus(SportsExternalResponse.ResponseStatus.IMPORTED);
        staging.setLinkedRegistration(savedReg);
        staging = responseRepo.save(staging);

        // 15. Send Confirmation Notification if email available
        try {
            if (savedReg.getEmail() != null && !savedReg.getEmail().isBlank()) {
                registrationEmailService.send(savedReg, requiresApproval
                        ? RegistrationEmailService.Stage.RECEIVED
                        : RegistrationEmailService.Stage.CONFIRMED);
            }
        } catch (Exception ex) {
            log.warn("Failed to dispatch registration email for imported external response: {}", ex.getMessage());
        }

        auditService.record(
                AuditAction.PLAYER_REGISTERED,
                AuditModule.SPORTS,
                "SportsEventRegistration",
                String.valueOf(savedReg.getId())
        );

        log.info("Successfully imported external response id={} into registration id={} with status={}",
                externalResponseId, savedReg.getId(), savedReg.getStatus());

        return staging;
    }

    private Map<String, String> extractCanonicalFields(
            List<SportsExternalFormFieldMapping> mappings,
            Map<String, Object> answersMap
    ) {
        Map<String, String> canonical = new HashMap<>();
        if (mappings == null || mappings.isEmpty()) {
            for (Map.Entry<String, Object> entry : answersMap.entrySet()) {
                canonical.put(entry.getKey(), entry.getValue() != null ? String.valueOf(entry.getValue()) : "");
            }
            return canonical;
        }

        for (SportsExternalFormFieldMapping m : mappings) {
            Object val = answersMap.get(m.getExternalQuestionId());
            if (val == null) {
                val = answersMap.get(m.getQuestionTitle());
            }
            if (val == null) {
                val = answersMap.get(m.getManaFieldKey());
            }

            if (val != null) {
                String strVal = String.valueOf(val).trim();
                if (m.getMappingType() == SportsExternalFormFieldMapping.MappingType.CATEGORY_LOOKUP
                        || m.getMappingType() == SportsExternalFormFieldMapping.MappingType.FORMAT_LOOKUP) {
                    if (m.getMappingRulesJson() != null && !m.getMappingRulesJson().isBlank()) {
                        try {
                            Map<String, String> ruleMap = objectMapper.readValue(
                                    m.getMappingRulesJson(),
                                    new TypeReference<Map<String, String>>() {}
                            );
                            if (ruleMap.containsKey(strVal)) {
                                strVal = ruleMap.get(strVal);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                canonical.put(m.getManaFieldKey(), strVal);
            }
        }
        return canonical;
    }

    private String normalizeGender(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String g = raw.trim().toUpperCase();
        if (g.equals("M") || g.startsWith("MALE") || g.startsWith("MEN") || g.startsWith("BOY")) return "MALE";
        if (g.equals("F") || g.startsWith("FEMALE") || g.startsWith("WOMEN") || g.startsWith("GIRL") || g.startsWith("LADIES")) return "FEMALE";
        return "OTHER";
    }

    private SportsEvent.MatchFormat resolveMatchFormat(String raw, SportsPlayerCategory category) {
        if (raw != null && !raw.isBlank()) {
            String clean = raw.trim().toUpperCase().replace(' ', '_');
            try {
                return SportsEvent.MatchFormat.valueOf(clean);
            } catch (IllegalArgumentException ignored) {}
            if (clean.contains("MIXED")) return SportsEvent.MatchFormat.MIXED_DOUBLES;
            if (clean.contains("DOUBLE")) return SportsEvent.MatchFormat.DOUBLES;
            if (clean.contains("SINGLE")) return SportsEvent.MatchFormat.SINGLES;
        }

        if (category != null && category.getName() != null) {
            String cn = category.getName().toUpperCase();
            if (cn.contains("MIXED")) return SportsEvent.MatchFormat.MIXED_DOUBLES;
            if (cn.contains("DOUBLES")) return SportsEvent.MatchFormat.DOUBLES;
        }
        return SportsEvent.MatchFormat.SINGLES;
    }
}
