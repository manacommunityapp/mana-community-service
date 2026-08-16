package com.manacommunity.api.events.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.events.dto.ActivityRegistrationRequest;
import com.manacommunity.api.events.dto.ActivityRegistrationResponse;
import com.manacommunity.api.events.entity.ActivityRegistration;
import com.manacommunity.api.events.entity.EventProgram;
import com.manacommunity.api.events.repository.ActivityRegistrationRepository;
import com.manacommunity.api.events.repository.EventProgramRepository;
import com.manacommunity.api.exception.AlreadyRegisteredException;
import com.manacommunity.api.exception.EventFullException;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityRegistrationService {

    private static final TypeReference<List<ActivityRegistrationResponse.ParticipantResponse>> PARTICIPANTS_TYPE =
            new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final EventProgramRepository programRepo;
    private final ActivityRegistrationRepository registrationRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public ActivityRegistrationResponse register(Long programId, ActivityRegistrationRequest req, AppUser user) {
        EventProgram program = getProgram(programId);
        String idempotencyKey = normalize(req.getIdempotencyKey());
        if (idempotencyKey != null) {
            var existing = registrationRepo.findByProgramIdAndUserIdAndIdempotencyKey(programId, user.getId(), idempotencyKey);
            if (existing.isPresent()) return toResponse(existing.get());
        }
        ActivityRegistration existingRegistration = registrationRepo.findByProgramIdAndUserId(programId, user.getId())
                .orElse(null);
        if (existingRegistration != null
                && existingRegistration.getStatus() != ActivityRegistration.ActivityRegStatus.CANCELLED) {
            throw new AlreadyRegisteredException(program.getTitle());
        }

        int headCount = resolveHeadCount(req);
        int maxPerRegistration = program.getMaxPerRegistration() > 0 ? program.getMaxPerRegistration() : 10;
        if (headCount > maxPerRegistration) {
            throw new InvalidInputException("Head count exceeds maximum allowed per registration: " + maxPerRegistration);
        }

        ActivityRegistration.ActivityRegStatus status = resolveSubmissionStatus(program, headCount);
        Integer waitlistPosition = null;
        if (status == ActivityRegistration.ActivityRegStatus.WAITLISTED) {
            waitlistPosition = registrationRepo.countByProgramIdAndStatus(programId, ActivityRegistration.ActivityRegStatus.WAITLISTED) + 1;
        }

        ActivityRegistration registration = existingRegistration != null
                ? existingRegistration
                : ActivityRegistration.builder().program(program).user(user).build();
        registration.setHeadCount(headCount);
        registration.setRegistrationType(normalize(req.getRegistrationType()) != null ? normalize(req.getRegistrationType()) : "individual");
        registration.setPrimaryName(resolvePrimaryName(req, user));
        registration.setPrimaryEmail(normalize(req.getPrimaryEmail()) != null ? normalize(req.getPrimaryEmail()) : user.getEmail());
        registration.setPrimaryPhone(normalize(req.getPrimaryPhone()));
        registration.setParticipantData(toJson(req.getParticipants()));
        registration.setCustomData(toJson(req.getCustomData()));
        registration.setIdempotencyKey(idempotencyKey);
        registration.setStatus(status);
        registration.setWaitlistPosition(waitlistPosition);
        registration.setDecisionReason(null);
        registration.setApprovedBy(null);
        registration.setApprovedAt(null);
        return toResponse(registrationRepo.save(registration));
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationResponse> getByProgram(Long programId) {
        return registrationRepo.findByProgramIdOrderByRegisteredAtDesc(programId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityRegistrationResponse> getMine(Long userId) {
        return registrationRepo.findByUserIdOrderByRegisteredAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ActivityRegistrationResponse approve(Long registrationId, AppUser approver) {
        ActivityRegistration registration = getRegistration(registrationId);
        if (registration.getStatus() == ActivityRegistration.ActivityRegStatus.CONFIRMED) {
            return toResponse(registration);
        }
        EventProgram program = registration.getProgram();
        if (!hasCapacity(program, registration.getHeadCount())) {
            if (program.isAllowWaitlist()) {
                registration.setStatus(ActivityRegistration.ActivityRegStatus.WAITLISTED);
                if (registration.getWaitlistPosition() == null) {
                    registration.setWaitlistPosition(registrationRepo.countByProgramIdAndStatus(
                            program.getId(), ActivityRegistration.ActivityRegStatus.WAITLISTED) + 1);
                }
                registration.setDecisionReason("Moved to waitlist because capacity is full");
                return toResponse(registrationRepo.save(registration));
            }
            throw new EventFullException(program.getTitle(), program.getCapacity());
        }
        registration.setStatus(ActivityRegistration.ActivityRegStatus.CONFIRMED);
        registration.setApprovedBy(approver);
        registration.setApprovedAt(LocalDateTime.now());
        registration.setDecisionReason(null);
        registration.setWaitlistPosition(null);
        return toResponse(registrationRepo.save(registration));
    }

    @Transactional
    public ActivityRegistrationResponse reject(Long registrationId, String reason, AppUser approver) {
        ActivityRegistration registration = getRegistration(registrationId);
        boolean wasConfirmed = registration.getStatus() == ActivityRegistration.ActivityRegStatus.CONFIRMED;
        registration.setStatus(ActivityRegistration.ActivityRegStatus.DECLINED);
        registration.setDecisionReason(normalize(reason));
        registration.setApprovedBy(approver);
        registration.setApprovedAt(LocalDateTime.now());
        ActivityRegistrationResponse response = toResponse(registrationRepo.save(registration));
        if (wasConfirmed) promoteNextWaitlisted(registration.getProgram());
        return response;
    }

    @Transactional
    public ActivityRegistrationResponse cancel(Long registrationId, AppUser user) {
        ActivityRegistration registration = getRegistration(registrationId);
        if (!registration.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("Only the registrant can cancel this activity registration");
        }
        boolean wasConfirmed = registration.getStatus() == ActivityRegistration.ActivityRegStatus.CONFIRMED;
        registration.setStatus(ActivityRegistration.ActivityRegStatus.CANCELLED);
        registration.setDecisionReason("Cancelled by registrant");
        ActivityRegistrationResponse response = toResponse(registrationRepo.save(registration));
        if (wasConfirmed) promoteNextWaitlisted(registration.getProgram());
        return response;
    }

    private ActivityRegistration.ActivityRegStatus resolveSubmissionStatus(EventProgram program, int headCount) {
        if (!hasCapacity(program, headCount)) {
            if (program.isAllowWaitlist()) return ActivityRegistration.ActivityRegStatus.WAITLISTED;
            throw new EventFullException(program.getTitle(), program.getCapacity());
        }
        return program.isRequiresApproval()
                ? ActivityRegistration.ActivityRegStatus.PENDING
                : ActivityRegistration.ActivityRegStatus.CONFIRMED;
    }

    private void promoteNextWaitlisted(EventProgram program) {
        registrationRepo.findFirstByProgramIdAndStatusOrderByWaitlistPositionAscRegisteredAtAsc(
                        program.getId(), ActivityRegistration.ActivityRegStatus.WAITLISTED)
                .filter(next -> hasCapacity(program, next.getHeadCount()))
                .ifPresent(next -> {
                    next.setStatus(program.isRequiresApproval()
                            ? ActivityRegistration.ActivityRegStatus.PENDING
                            : ActivityRegistration.ActivityRegStatus.CONFIRMED);
                    next.setDecisionReason("Promoted from waitlist");
                    next.setWaitlistPosition(null);
                    registrationRepo.save(next);
                });
    }

    private boolean hasCapacity(EventProgram program, int requestedHeadCount) {
        if (program.getCapacity() == null || program.getCapacity() <= 0) return true;
        int confirmed = registrationRepo.sumHeadCountByProgramIdAndStatus(
                program.getId(), ActivityRegistration.ActivityRegStatus.CONFIRMED);
        return confirmed + requestedHeadCount <= program.getCapacity();
    }

    private int spotsLeft(EventProgram program) {
        if (program.getCapacity() == null || program.getCapacity() <= 0) return -1;
        int confirmed = registrationRepo.sumHeadCountByProgramIdAndStatus(
                program.getId(), ActivityRegistration.ActivityRegStatus.CONFIRMED);
        return Math.max(0, program.getCapacity() - confirmed);
    }

    private int resolveHeadCount(ActivityRegistrationRequest req) {
        if (req.getParticipants() != null && !req.getParticipants().isEmpty()) {
            return req.getParticipants().size();
        }
        return req.getHeadCount() != null && req.getHeadCount() > 0 ? req.getHeadCount() : 1;
    }

    private String resolvePrimaryName(ActivityRegistrationRequest req, AppUser user) {
        String primaryName = normalize(req.getPrimaryName());
        if (primaryName != null) return primaryName;
        if (req.getParticipants() != null && !req.getParticipants().isEmpty()) {
            String participantName = normalize(req.getParticipants().get(0).getFullName());
            if (participantName != null) return participantName;
        }
        return user.getFullName();
    }

    private EventProgram getProgram(Long programId) {
        return programRepo.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", programId));
    }

    private ActivityRegistration getRegistration(Long registrationId) {
        return registrationRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity registration", registrationId));
    }

    private ActivityRegistrationResponse toResponse(ActivityRegistration registration) {
        ActivityRegistrationResponse response = new ActivityRegistrationResponse();
        EventProgram program = registration.getProgram();
        response.setId(registration.getId());
        response.setProgramId(program.getId());
        response.setProgramTitle(program.getTitle());
        response.setEventId(program.getEvent().getId());
        response.setEventTitle(program.getEvent().getTitle());
        response.setUserId(registration.getUser().getId());
        response.setUserName(registration.getUser().getFullName());
        response.setUserEmail(registration.getUser().getEmail());
        response.setHeadCount(registration.getHeadCount() != null ? registration.getHeadCount() : 1);
        response.setRegistrationType(registration.getRegistrationType());
        response.setPrimaryName(registration.getPrimaryName());
        response.setPrimaryEmail(registration.getPrimaryEmail());
        response.setPrimaryPhone(registration.getPrimaryPhone());
        response.setStatus(registration.getStatus().name());
        response.setSpotsLeft(spotsLeft(program));
        response.setWaitlistPosition(registration.getWaitlistPosition());
        response.setDecisionReason(registration.getDecisionReason());
        if (registration.getApprovedBy() != null) {
            response.setApprovedById(registration.getApprovedBy().getId());
            response.setApprovedByName(registration.getApprovedBy().getFullName());
        }
        response.setApprovedAt(formatDt(registration.getApprovedAt()));
        response.setRegisteredAt(formatDt(registration.getRegisteredAt()));
        response.setParticipants(fromJson(registration.getParticipantData(), PARTICIPANTS_TYPE, List.of()));
        response.setCustomData(fromJson(registration.getCustomData(), MAP_TYPE, Map.of()));
        return response;
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new InvalidInputException("Invalid registration data");
        }
    }

    private <T> T fromJson(String value, TypeReference<T> type, T fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException e) {
            return fallback;
        }
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatDt(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }
}
