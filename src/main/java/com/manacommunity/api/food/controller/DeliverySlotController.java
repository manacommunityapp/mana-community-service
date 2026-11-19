package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.DeliverySlot;
import com.manacommunity.api.food.entity.DeliverySlotTemplate;
import com.manacommunity.api.food.repository.DeliverySlotTemplateRepository;
import com.manacommunity.api.food.service.DeliverySlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/food/slots")
@RequiredArgsConstructor
public class DeliverySlotController {

    private final DeliverySlotService slotService;
    private final DeliverySlotTemplateRepository templateRepo;

    @GetMapping
    public ResponseEntity<List<DeliverySlot>> getAvailableSlots(
            @RequestParam Long vendorId,
            @RequestParam Long communityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotService.getAvailableSlots(vendorId, communityId, date));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<DeliverySlotTemplate>> getTemplates(
            @RequestParam Long vendorId, @RequestParam Long communityId) {
        return ResponseEntity.ok(templateRepo.findByVendorIdAndCommunityIdAndIsActiveTrue(vendorId, communityId));
    }

    @PostMapping("/templates")
    public ResponseEntity<DeliverySlotTemplate> createTemplate(@RequestBody DeliverySlotTemplate template) {
        template.setActive(true);
        return ResponseEntity.status(201).body(templateRepo.save(template));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<DeliverySlotTemplate> updateTemplate(@PathVariable Long id, @RequestBody DeliverySlotTemplate body) {
        return templateRepo.findById(id).map(t -> {
            t.setSlotStart(body.getSlotStart());
            t.setSlotEnd(body.getSlotEnd());
            t.setCapacity(body.getCapacity());
            t.setCutoffHours(body.getCutoffHours());
            t.setActive(body.isActive());
            return ResponseEntity.ok(templateRepo.save(t));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/book")
    public ResponseEntity<Void> bookSlot(@PathVariable Long id) {
        slotService.bookSlot(id);
        return ResponseEntity.ok().build();
    }
}
