package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.OrderRefund;
import com.manacommunity.api.food.entity.OrderRefund.RefundStatus;
import com.manacommunity.api.food.entity.OrderRefund.RefundTrigger;
import com.manacommunity.api.food.repository.OrderRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderRefundService {

    private static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("500.00");

    private final OrderRefundRepository refundRepo;

    @Transactional
    public OrderRefund initiateRefund(Long communityId, Long orderId, String orderType,
            Long userId, RefundTrigger trigger, BigDecimal amount, String itemDetails) {
        OrderRefund refund = OrderRefund.builder()
            .communityId(communityId).orderId(orderId).orderType(orderType)
            .userId(userId).triggerType(trigger).amount(amount).itemDetails(itemDetails)
            .status(RefundStatus.INITIATED).autoApproved(false).createdAt(Instant.now()).build();
        // Auto-approve for OOS/substitution rejection up to ₹500
        if ((trigger == RefundTrigger.OUT_OF_STOCK || trigger == RefundTrigger.SUBSTITUTION_REJECTED)
                && amount.compareTo(AUTO_APPROVE_LIMIT) <= 0) {
            refund.setAutoApproved(true);
            refund.setStatus(RefundStatus.APPROVED);
            refund.setReviewedAt(Instant.now());
        }
        return refundRepo.save(refund);
    }

    @Transactional
    public OrderRefund processRefund(Long refundId, Long adminId, boolean approve, String notes) {
        OrderRefund refund = refundRepo.findById(refundId).orElseThrow();
        refund.setReviewedBy(adminId);
        refund.setReviewedAt(Instant.now());
        refund.setNotes(notes);
        refund.setStatus(approve ? RefundStatus.APPROVED : RefundStatus.REJECTED);
        return refundRepo.save(refund);
    }

    public List<OrderRefund> getPendingRefunds(Long communityId) {
        return refundRepo.findByCommunityIdAndStatus(communityId, RefundStatus.INITIATED);
    }

    public List<OrderRefund> getAllRefunds(Long communityId) {
        return refundRepo.findByCommunityId(communityId);
    }
}
