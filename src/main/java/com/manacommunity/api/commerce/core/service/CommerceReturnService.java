package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceReturnDto;
import com.manacommunity.api.commerce.core.dto.CommerceReturnRequest;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceReturn;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommerceReturnRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommerceReturnService {

    private final CommerceReturnRepository returnRepository;
    private final CommerceOrderRepository orderRepository;
    private final AppUserRepository userRepository;

    @Transactional
    public CommerceReturnDto createReturn(AppUser customer, CommerceReturnRequest req) {
        CommerceOrder order = orderRepository.findByOrderNumber(req.getOrderNumber())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + req.getOrderNumber()));
        if (!order.getBuyer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("Not your order.");
        }

        CommerceReturn ret = CommerceReturn.builder()
                .returnCode("RET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .order(order)
                .orderNumber(order.getOrderNumber())
                .customerUser(customer)
                .community(customer.getCommunity())
                .returnType(req.getReturnType() != null ? req.getReturnType() : "RETURN")
                .reason(req.getReason() != null ? req.getReason() : "Not specified")
                .description(req.getDescription())
                .itemsJson(req.getItemsJson())
                .refundAmount(req.getRefundAmount())
                .build();

        return toDto(returnRepository.save(ret));
    }

    @Transactional(readOnly = true)
    public List<CommerceReturnDto> getMyReturns(AppUser customer) {
        return returnRepository.findByCustomerUserIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommerceReturnDto> getAdminReturns(AppUser admin, String status) {
        Long communityId = admin.getCommunity().getId();
        List<CommerceReturn> list = (status != null && !status.isBlank())
                ? returnRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status.toUpperCase())
                : returnRepository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommerceReturnDto resolveReturn(AppUser admin, String returnCode, String status, String adminNote) {
        CommerceReturn ret = returnRepository.findByReturnCode(returnCode)
                .orElseThrow(() -> new IllegalArgumentException("Return not found: " + returnCode));
        if (!ret.getCommunity().getId().equals(admin.getCommunity().getId())) {
            throw new AccessDeniedException("Return not in your community.");
        }
        ret.setStatus(status.toUpperCase());
        ret.setAdminNote(adminNote);
        ret.setResolvedBy(admin);
        ret.setResolvedAt(LocalDateTime.now());
        ret.setUpdatedAt(LocalDateTime.now());
        return toDto(returnRepository.save(ret));
    }

    private CommerceReturnDto toDto(CommerceReturn r) {
        return CommerceReturnDto.builder()
                .id(r.getId()).returnCode(r.getReturnCode())
                .orderId(r.getOrder().getId()).orderNumber(r.getOrderNumber())
                .customerId(r.getCustomerUser().getId()).customerName(r.getCustomerUser().getFullName())
                .returnType(r.getReturnType()).reason(r.getReason())
                .description(r.getDescription()).itemsJson(r.getItemsJson())
                .refundAmount(r.getRefundAmount()).status(r.getStatus())
                .adminNote(r.getAdminNote())
                .resolvedByName(r.getResolvedBy() != null ? r.getResolvedBy().getFullName() : null)
                .resolvedAt(r.getResolvedAt() != null ? r.getResolvedAt().toString() : null)
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build();
    }
}
