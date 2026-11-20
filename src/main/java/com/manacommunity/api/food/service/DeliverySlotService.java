package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.DeliverySlot;
import com.manacommunity.api.food.entity.DeliverySlot.SlotStatus;
import com.manacommunity.api.food.entity.DeliverySlotTemplate;
import com.manacommunity.api.food.repository.DeliverySlotRepository;
import com.manacommunity.api.food.repository.DeliverySlotTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliverySlotService {

    private final DeliverySlotRepository slotRepo;
    private final DeliverySlotTemplateRepository templateRepo;

    @Transactional
    public void generateSlotsForDate(Long vendorId, Long communityId, LocalDate date) {
        List<DeliverySlotTemplate> templates = templateRepo.findByVendorIdAndCommunityIdAndIsActiveTrue(vendorId, communityId);
        int targetDow = date.getDayOfWeek().getValue(); // 1=Mon…7=Sun
        for (DeliverySlotTemplate t : templates) {
            if (t.getDayOfWeek() != targetDow) continue;
            Instant start = date.atTime(t.getSlotStart()).toInstant(ZoneOffset.UTC);
            Instant end = date.atTime(t.getSlotEnd()).toInstant(ZoneOffset.UTC);
            Instant cutoff = start.minus(Duration.ofHours(t.getCutoffHours()));
            DeliverySlot slot = DeliverySlot.builder()
                .templateId(t.getId()).vendorId(vendorId).communityId(communityId)
                .slotDate(date).slotStart(start).slotEnd(end).cutoffAt(cutoff)
                .capacity(t.getCapacity()).bookedCount(0).status(SlotStatus.OPEN)
                .createdAt(Instant.now()).build();
            try {
                slotRepo.save(slot);
            } catch (Exception e) {
                log.debug("Slot already exists for vendor={} date={} start={}", vendorId, date, start);
            }
        }
    }

    public List<DeliverySlot> getAvailableSlots(Long vendorId, Long communityId, LocalDate date) {
        return slotRepo.findByVendorIdAndCommunityIdAndSlotDateAndStatus(vendorId, communityId, date, SlotStatus.OPEN)
            .stream().filter(s -> s.getCutoffAt().isAfter(Instant.now())).toList();
    }

    @Transactional
    public void bookSlot(Long slotId) {
        int updated = slotRepo.incrementBookedCount(slotId);
        if (updated == 0) throw new IllegalStateException("Slot is full or not available");
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void generateUpcomingSlots() {
        List<DeliverySlotTemplate> templates = templateRepo.findByIsActiveTrue();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);
            templates.stream()
                .map(t -> new long[]{t.getVendorId(), t.getCommunityId()})
                .distinct()
                .forEach(vc -> generateSlotsForDate(vc[0], vc[1], date));
        }
        log.info("[DeliverySlot] Generated slots for next 7 days");
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void markCutoffPassed() {
        List<DeliverySlot> due = slotRepo.findByStatusAndCutoffAtBefore(SlotStatus.OPEN, Instant.now());
        for (DeliverySlot s : due) {
            s.setStatus(SlotStatus.CUTOFF_PASSED);
            slotRepo.save(s);
        }
        if (!due.isEmpty()) log.info("[DeliverySlot] Marked {} slots as CUTOFF_PASSED", due.size());
    }
}
