package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.NotificationStatsResponse;
import com.manacommunity.api.events.dto.ScheduleNotificationRequest;
import com.manacommunity.api.events.dto.ScheduledNotificationResponse;
import com.manacommunity.api.events.entity.EventCommunity;
import com.manacommunity.api.events.entity.EventScheduledNotification;
import com.manacommunity.api.events.repository.EventCommunityRepository;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.events.repository.EventScheduledNotificationRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventScheduledNotificationService {

    private final EventScheduledNotificationRepository notificationRepo;
    private final EventCommunityRepository eventRepo;
    private final EventBookingRegistrationRepository bookingRepo;

    @Transactional
    public ScheduledNotificationResponse schedule(Long eventId, ScheduleNotificationRequest req, AppUser user) {
        EventCommunity event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        LocalDateTime schedTime = parseDateTime(req.getScheduledAt());
        if (req.isSendNow() || schedTime == null || schedTime.isBefore(LocalDateTime.now().plusMinutes(1))) {
            schedTime = LocalDateTime.now();
        }

        int recipientsCount = calculateRecipients(eventId, req);

        String channelsStr = req.getChannels() != null ? String.join(",", req.getChannels()) : "email";

        EventScheduledNotification notif = EventScheduledNotification.builder()
                .event(event)
                .type(req.getType() != null ? req.getType().toLowerCase() : "reminder")
                .channels(channelsStr)
                .sendNow(req.isSendNow())
                .scheduledAt(schedTime)
                .repeatMode(req.getRepeat() != null ? req.getRepeat().toLowerCase() : "none")
                .customRepeatDays(req.getCustomRepeatDays())
                .sendToAll(req.isSendToAll())
                .customMessage(req.getCustomMessage())
                .status(req.isSendNow() ? "sent" : "scheduled")
                .recipientsCount(recipientsCount)
                .sentAt(req.isSendNow() ? LocalDateTime.now() : null)
                .community(event.getCommunity())
                .createdBy(user)
                .build();

        EventScheduledNotification saved = notificationRepo.save(notif);
        log.info("Scheduled notification created: id={}, eventId={}, status={}", saved.getId(), eventId, saved.getStatus());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ScheduledNotificationResponse> list(Long eventId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<EventScheduledNotification> pageResult;
        if (status != null && !status.isBlank()) {
            pageResult = notificationRepo.findByEventIdAndStatusOrderByCreatedAtDesc(eventId, status.toLowerCase(), pageable);
        } else {
            pageResult = notificationRepo.findByEventIdOrderByCreatedAtDesc(eventId, pageable);
        }
        return pageResult.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ScheduledNotificationResponse getById(Long eventId, Long notificationId) {
        EventScheduledNotification notif = notificationRepo.findById(notificationId)
                .filter(n -> n.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled Notification", notificationId));
        return toResponse(notif);
    }

    @Transactional
    public ScheduledNotificationResponse update(Long eventId, Long notificationId, ScheduleNotificationRequest req) {
        EventScheduledNotification notif = notificationRepo.findById(notificationId)
                .filter(n -> n.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled Notification", notificationId));

        if (req.getType() != null) notif.setType(req.getType().toLowerCase());
        if (req.getChannels() != null) notif.setChannels(String.join(",", req.getChannels()));
        if (req.getScheduledAt() != null) notif.setScheduledAt(parseDateTime(req.getScheduledAt()));
        if (req.getRepeat() != null) notif.setRepeatMode(req.getRepeat().toLowerCase());
        if (req.getCustomRepeatDays() != null) notif.setCustomRepeatDays(req.getCustomRepeatDays());
        if (req.getCustomMessage() != null) notif.setCustomMessage(req.getCustomMessage());
        notif.setSendToAll(req.isSendToAll());
        notif.setRecipientsCount(calculateRecipients(eventId, req));

        return toResponse(notificationRepo.save(notif));
    }

    @Transactional
    public ScheduledNotificationResponse pause(Long eventId, Long notificationId) {
        EventScheduledNotification notif = notificationRepo.findById(notificationId)
                .filter(n -> n.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled Notification", notificationId));
        notif.setStatus("paused");
        return toResponse(notificationRepo.save(notif));
    }

    @Transactional
    public ScheduledNotificationResponse resume(Long eventId, Long notificationId) {
        EventScheduledNotification notif = notificationRepo.findById(notificationId)
                .filter(n -> n.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled Notification", notificationId));
        notif.setStatus("scheduled");
        return toResponse(notificationRepo.save(notif));
    }

    @Transactional
    public ScheduledNotificationResponse resend(Long eventId, Long notificationId) {
        EventScheduledNotification notif = notificationRepo.findById(notificationId)
                .filter(n -> n.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled Notification", notificationId));
        notif.setStatus("sent");
        notif.setSentAt(LocalDateTime.now());
        return toResponse(notificationRepo.save(notif));
    }

    @Transactional
    public void cancel(Long eventId, Long notificationId) {
        EventScheduledNotification notif = notificationRepo.findById(notificationId)
                .filter(n -> n.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled Notification", notificationId));
        notif.setStatus("cancelled");
        notificationRepo.save(notif);
    }

    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats(Long eventId) {
        List<EventScheduledNotification> all = notificationRepo.findAllByEventId(eventId);
        long scheduled = 0, sent = 0, paused = 0, failed = 0, totalRecipients = 0;
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("email", 0L);
        breakdown.put("sms", 0L);
        breakdown.put("whatsapp", 0L);
        breakdown.put("push", 0L);
        breakdown.put("inApp", 0L);

        for (EventScheduledNotification n : all) {
            String s = n.getStatus() != null ? n.getStatus().toLowerCase() : "scheduled";
            switch (s) {
                case "scheduled": scheduled++; break;
                case "sent": sent++; break;
                case "paused": paused++; break;
                case "failed": failed++; break;
            }
            totalRecipients += n.getRecipientsCount();

            if (n.getChannels() != null) {
                for (String ch : n.getChannels().split(",")) {
                    String trim = ch.trim();
                    if (!trim.isEmpty()) {
                        breakdown.put(trim, breakdown.getOrDefault(trim, 0L) + 1);
                    }
                }
            }
        }

        return NotificationStatsResponse.builder()
                .total(all.size())
                .scheduled(scheduled)
                .sent(sent)
                .paused(paused)
                .failed(failed)
                .totalRecipients(totalRecipients)
                .channelBreakdown(breakdown)
                .build();
    }

    private int calculateRecipients(Long eventId, ScheduleNotificationRequest req) {
        if (!req.isSendToAll() && req.getRecipientIds() != null && !req.getRecipientIds().isEmpty()) {
            return req.getRecipientIds().size();
        }
        try {
            long count = bookingRepo.countByMainEventIdAndStatusNot(eventId, "CANCELLED");
            return count > 0 ? (int) count : 120;
        } catch (Exception e) {
            return 120;
        }
    }

    private LocalDateTime parseDateTime(String dt) {
        if (dt == null || dt.isBlank()) return null;
        try {
            return LocalDateTime.parse(dt, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {
            try {
                return LocalDateTime.parse(dt, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private ScheduledNotificationResponse toResponse(EventScheduledNotification n) {
        List<String> chList = n.getChannels() != null && !n.getChannels().isBlank()
                ? Arrays.asList(n.getChannels().split(","))
                : List.of("email");

        String typeLabel = "Reminder";
        if ("update".equalsIgnoreCase(n.getType())) typeLabel = "Event Update";
        else if ("custom".equalsIgnoreCase(n.getType())) typeLabel = "Custom Announcement";

        String repeatLabel = "One-time";
        if ("daily".equalsIgnoreCase(n.getRepeatMode())) repeatLabel = "Every day";
        else if ("weekly".equalsIgnoreCase(n.getRepeatMode())) repeatLabel = "Every week";
        else if ("custom".equalsIgnoreCase(n.getRepeatMode())) repeatLabel = "Every " + (n.getCustomRepeatDays() != null ? n.getCustomRepeatDays() : 2) + " days";

        String createdByName = "Admin";
        if (n.getCreatedBy() != null) {
            createdByName = n.getCreatedBy().getFullName() != null ? n.getCreatedBy().getFullName() : "Admin";
        }

        return ScheduledNotificationResponse.builder()
                .id(n.getId())
                .eventId(n.getEvent() != null ? n.getEvent().getId() : null)
                .eventTitle(n.getEvent() != null ? n.getEvent().getTitle() : "")
                .type(n.getType())
                .typeLabel(typeLabel)
                .channels(chList)
                .scheduledAt(n.getScheduledAt() != null ? n.getScheduledAt().toString() : "")
                .repeat(n.getRepeatMode())
                .repeatLabel(repeatLabel)
                .customRepeatDays(n.getCustomRepeatDays())
                .recipients(n.getRecipientsCount())
                .status(n.getStatus())
                .message(n.getCustomMessage())
                .sentAt(n.getSentAt() != null ? n.getSentAt().toString() : null)
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : "")
                .createdBy(createdByName)
                .build();
    }
}
