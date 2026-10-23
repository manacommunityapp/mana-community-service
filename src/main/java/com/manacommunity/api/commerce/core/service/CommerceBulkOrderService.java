package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceBulkOrderDto;
import com.manacommunity.api.commerce.core.dto.CommerceBulkOrderRequest;
import com.manacommunity.api.commerce.core.model.CommerceBulkOrder;
import com.manacommunity.api.commerce.core.repository.CommerceBulkOrderRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceBulkOrderService {

    private final CommerceBulkOrderRepository bulkOrderRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public CommerceBulkOrderDto createBulkOrder(AppUser customer, CommerceBulkOrderRequest req) {
        AppUser vendor = req.getVendorUserId() != null
                ? userRepository.findById(req.getVendorUserId()).orElse(null) : null;

        CommerceBulkOrder bulk = CommerceBulkOrder.builder()
                .bulkOrderCode("BULK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .community(customer.getCommunity())
                .customerUser(customer)
                .vendorUser(vendor)
                .itemsJson(req.getItemsJson() != null ? req.getItemsJson() : "[]")
                .deliveryAddress(req.getDeliveryAddress())
                .requestedDate(req.getRequestedDate() != null ? LocalDate.parse(req.getRequestedDate()) : null)
                .customerNote(req.getCustomerNote())
                .build();

        return toDto(bulkOrderRepository.save(bulk));
    }

    @Transactional(readOnly = true)
    public List<CommerceBulkOrderDto> getMyBulkOrders(AppUser customer) {
        return bulkOrderRepository.findByCustomerUserIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommerceBulkOrderDto> getAdminBulkOrders(AppUser admin) {
        return bulkOrderRepository.findByCommunityIdOrderByCreatedAtDesc(admin.getCommunity().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommerceBulkOrderDto updateBulkOrder(AppUser admin, String code, CommerceBulkOrderRequest req) {
        CommerceBulkOrder bulk = findForAdmin(admin, code);
        if (req.getQuotedAmount() != null) bulk.setQuotedAmount(req.getQuotedAmount());
        if (req.getAdminNote() != null) bulk.setAdminNote(req.getAdminNote());
        if (req.getVendorNote() != null) bulk.setVendorNote(req.getVendorNote());
        bulk.setUpdatedAt(LocalDateTime.now());
        return toDto(bulkOrderRepository.save(bulk));
    }

    @Transactional
    public CommerceBulkOrderDto updateStatus(AppUser admin, String code, String status) {
        CommerceBulkOrder bulk = findForAdmin(admin, code);
        bulk.setStatus(status.toUpperCase());
        if ("APPROVED".equals(status.toUpperCase())) {
            bulk.setApprovedBy(admin);
            bulk.setApprovedAt(LocalDateTime.now());
        }
        bulk.setUpdatedAt(LocalDateTime.now());
        return toDto(bulkOrderRepository.save(bulk));
    }

    @Transactional
    public CommerceBulkOrderDto cancelBulkOrder(AppUser customer, String code) {
        CommerceBulkOrder bulk = bulkOrderRepository.findByBulkOrderCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Bulk order not found: " + code));
        if (!bulk.getCustomerUser().getId().equals(customer.getId())) {
            throw new AccessDeniedException("Not your bulk order.");
        }
        bulk.setStatus("CANCELLED");
        bulk.setUpdatedAt(LocalDateTime.now());
        return toDto(bulkOrderRepository.save(bulk));
    }

    private CommerceBulkOrder findForAdmin(AppUser admin, String code) {
        CommerceBulkOrder bulk = bulkOrderRepository.findByBulkOrderCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Bulk order not found: " + code));
        if (!bulk.getCommunity().getId().equals(admin.getCommunity().getId())) {
            throw new AccessDeniedException("Not in your community.");
        }
        return bulk;
    }

    private CommerceBulkOrderDto toDto(CommerceBulkOrder b) {
        return CommerceBulkOrderDto.builder()
                .id(b.getId()).bulkOrderCode(b.getBulkOrderCode())
                .customerId(b.getCustomerUser().getId()).customerName(b.getCustomerUser().getFullName())
                .vendorId(b.getVendorUser() != null ? b.getVendorUser().getId() : null)
                .vendorName(b.getVendorUser() != null ? b.getVendorUser().getFullName() : null)
                .itemsJson(b.getItemsJson()).quotedAmount(b.getQuotedAmount()).finalAmount(b.getFinalAmount())
                .deliveryAddress(b.getDeliveryAddress())
                .requestedDate(b.getRequestedDate() != null ? b.getRequestedDate().toString() : null)
                .status(b.getStatus()).customerNote(b.getCustomerNote())
                .adminNote(b.getAdminNote()).vendorNote(b.getVendorNote())
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null)
                .build();
    }
}
