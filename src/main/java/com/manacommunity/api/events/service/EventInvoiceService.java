package com.manacommunity.api.events.service;

import com.manacommunity.api.events.dto.EventInvoiceRequest;
import com.manacommunity.api.events.dto.EventInvoiceResponse;
import com.manacommunity.api.events.entity.CommunityEvent;
import com.manacommunity.api.events.entity.EventInvoice;
import com.manacommunity.api.events.repository.CommunityEventRepository;
import com.manacommunity.api.events.repository.EventInvoiceRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventInvoiceService {

    private final EventInvoiceRepository invoiceRepo;
    private final CommunityEventRepository eventRepo;

    @Transactional(readOnly = true)
    public List<EventInvoiceResponse> getByEvent(Long eventId) {
        return invoiceRepo.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventInvoiceResponse> getByCommunity(Long communityId) {
        return invoiceRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EventInvoiceResponse create(EventInvoiceRequest req, AppUser user, Community community) {
        CommunityEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        BigDecimal amount = req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO;
        BigDecimal tax = req.getTaxAmount() != null ? req.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal total = req.getTotalAmount() != null ? req.getTotalAmount() : amount.add(tax);

        EventInvoice invoice = EventInvoice.builder()
                .event(event)
                .community(community)
                .createdBy(user)
                .invoiceNumber(req.getInvoiceNumber())
                .vendorName(req.getVendorName())
                .invoiceDate(parseDate(req.getInvoiceDate()))
                .dueDate(parseDate(req.getDueDate()))
                .amount(amount)
                .taxAmount(tax)
                .totalAmount(total)
                .category(req.getCategory() != null ? req.getCategory() : "OTHER")
                .status(req.getStatus() != null ? req.getStatus() : "PENDING")
                .invoiceUrl(req.getInvoiceUrl())
                .fileId(req.getFileId())
                .notes(req.getNotes())
                .build();

        return toResponse(invoiceRepo.save(invoice));
    }

    @Transactional
    public EventInvoiceResponse update(Long id, EventInvoiceRequest req) {
        EventInvoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        BigDecimal amount = req.getAmount() != null ? req.getAmount() : inv.getAmount();
        BigDecimal tax = req.getTaxAmount() != null ? req.getTaxAmount() : inv.getTaxAmount();
        BigDecimal total = req.getTotalAmount() != null ? req.getTotalAmount() : amount.add(tax);

        inv.setInvoiceNumber(req.getInvoiceNumber());
        inv.setVendorName(req.getVendorName());
        inv.setInvoiceDate(parseDate(req.getInvoiceDate()));
        inv.setDueDate(parseDate(req.getDueDate()));
        inv.setAmount(amount);
        inv.setTaxAmount(tax);
        inv.setTotalAmount(total);
        inv.setCategory(req.getCategory() != null ? req.getCategory() : inv.getCategory());
        inv.setStatus(req.getStatus() != null ? req.getStatus() : inv.getStatus());
        inv.setInvoiceUrl(req.getInvoiceUrl());
        inv.setFileId(req.getFileId());
        inv.setNotes(req.getNotes());

        return toResponse(invoiceRepo.save(inv));
    }

    @Transactional
    public EventInvoiceResponse updateStatus(Long id, String status) {
        EventInvoice inv = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        inv.setStatus(status);
        return toResponse(invoiceRepo.save(inv));
    }

    @Transactional
    public void delete(Long id) {
        invoiceRepo.deleteById(id);
    }

    private EventInvoiceResponse toResponse(EventInvoice inv) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return EventInvoiceResponse.builder()
                .id(inv.getId())
                .eventId(inv.getEvent().getId())
                .eventTitle(inv.getEvent().getTitle())
                .invoiceNumber(inv.getInvoiceNumber())
                .vendorName(inv.getVendorName())
                .invoiceDate(inv.getInvoiceDate() != null ? inv.getInvoiceDate().toString() : null)
                .dueDate(inv.getDueDate() != null ? inv.getDueDate().toString() : null)
                .amount(inv.getAmount())
                .taxAmount(inv.getTaxAmount())
                .totalAmount(inv.getTotalAmount())
                .category(inv.getCategory())
                .status(inv.getStatus())
                .invoiceUrl(inv.getInvoiceUrl())
                .fileId(inv.getFileId())
                .notes(inv.getNotes())
                .createdByName(inv.getCreatedBy() != null ? inv.getCreatedBy().getFullName() : null)
                .createdAt(inv.getCreatedAt() != null ? inv.getCreatedAt().format(fmt) : null)
                .updatedAt(inv.getUpdatedAt() != null ? inv.getUpdatedAt().format(fmt) : null)
                .build();
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s);
    }
}
