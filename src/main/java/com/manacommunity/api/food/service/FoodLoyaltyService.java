package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodLoyaltyCoupon;
import com.manacommunity.api.food.entity.FoodLoyaltyCouponUsage;
import com.manacommunity.api.food.entity.FoodLoyaltyGiftCard;
import com.manacommunity.api.food.entity.FoodLoyaltyMember;
import com.manacommunity.api.food.entity.FoodLoyaltyProgram;
import com.manacommunity.api.food.entity.FoodLoyaltyTransaction;
import com.manacommunity.api.food.repository.FoodLoyaltyCouponRepository;
import com.manacommunity.api.food.repository.FoodLoyaltyMemberRepository;
import com.manacommunity.api.food.repository.FoodLoyaltyProgramRepository;
import com.manacommunity.api.food.repository.FoodLoyaltyTransactionRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodLoyaltyService {

    private final FoodLoyaltyProgramRepository programRepo;
    private final FoodLoyaltyMemberRepository memberRepo;
    private final FoodLoyaltyTransactionRepository transactionRepo;
    private final FoodLoyaltyCouponRepository couponRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list(Long communityId) {
        List<FoodLoyaltyProgram> programs = programRepo.findByCommunityIdAndStatus(communityId,
                FoodLoyaltyProgram.ProgramStatus.ACTIVE.name());
        return programs.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> enroll(Long communityId, Long programId, AppUser user) {
        Community community = user.getCommunity();
        FoodLoyaltyProgram program = programRepo.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("LoyaltyProgram", programId));

        FoodLoyaltyMember member = FoodLoyaltyMember.builder()
                .program(program)
                .user(user)
                .pointsBalance(0)
                .lifetimePoints(0)
                .tier(FoodLoyaltyMember.MemberTier.BRONZE)
                .joinedAt(LocalDateTime.now())
                .community(community)
                .build();

        FoodLoyaltyMember saved = memberRepo.save(member);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("programId", saved.getProgram().getId());
        map.put("programName", saved.getProgram().getName());
        map.put("userId", saved.getUser().getId());
        map.put("pointsBalance", saved.getPointsBalance());
        map.put("lifetimePoints", saved.getLifetimePoints());
        map.put("tier", saved.getTier() != null ? saved.getTier().name() : null);
        map.put("joinedAt", saved.getJoinedAt());
        map.put("communityId", saved.getCommunity() != null ? saved.getCommunity().getId() : null);
        map.put("createdAt", saved.getCreatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMemberInfo(Long communityId, Long userId) {
        List<FoodLoyaltyMember> members = memberRepo.findByUserIdAndCommunityId(userId, communityId);
        return members.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("programId", m.getProgram().getId());
            map.put("programName", m.getProgram().getName());
            map.put("userId", m.getUser().getId());
            map.put("pointsBalance", m.getPointsBalance());
            map.put("lifetimePoints", m.getLifetimePoints());
            map.put("tier", m.getTier() != null ? m.getTier().name() : null);
            map.put("joinedAt", m.getJoinedAt());
            map.put("communityId", m.getCommunity() != null ? m.getCommunity().getId() : null);
            map.put("createdAt", m.getCreatedAt());
            map.put("updatedAt", m.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> earnPoints(Long communityId, Long userId, Integer points, String referenceType, Long referenceId) {
        List<FoodLoyaltyMember> members = memberRepo.findByUserIdAndCommunityId(userId, communityId);
        if (members.isEmpty()) {
            throw new ResourceNotFoundException("LoyaltyMember", "userId", userId.toString());
        }

        FoodLoyaltyMember member = members.get(0);
        member.setPointsBalance(member.getPointsBalance() + points);
        member.setLifetimePoints(member.getLifetimePoints() + points);
        memberRepo.save(member);

        FoodLoyaltyTransaction txn = FoodLoyaltyTransaction.builder()
                .member(member)
                .transactionType(FoodLoyaltyTransaction.LoyaltyTransactionType.EARN)
                .points(points)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .description("Earned " + points + " points for " + referenceType)
                .build();

        FoodLoyaltyTransaction savedTxn = transactionRepo.save(txn);

        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", savedTxn.getId());
        map.put("memberId", member.getId());
        map.put("pointsEarned", points);
        map.put("currentBalance", member.getPointsBalance());
        map.put("lifetimePoints", member.getLifetimePoints());
        map.put("tier", member.getTier() != null ? member.getTier().name() : null);
        return map;
    }

    @Transactional
    public Map<String, Object> redeemPoints(Long communityId, Long userId, Integer points) {
        List<FoodLoyaltyMember> members = memberRepo.findByUserIdAndCommunityId(userId, communityId);
        if (members.isEmpty()) {
            throw new ResourceNotFoundException("LoyaltyMember", "userId", userId.toString());
        }

        FoodLoyaltyMember member = members.get(0);

        if (member.getPointsBalance() < points) {
            throw new IllegalArgumentException("Insufficient points balance. Available: " + member.getPointsBalance());
        }

        FoodLoyaltyProgram program = member.getProgram();
        if (program.getMinRedeemPoints() != null && points < program.getMinRedeemPoints()) {
            throw new IllegalArgumentException("Minimum redeem points is " + program.getMinRedeemPoints());
        }

        member.setPointsBalance(member.getPointsBalance() - points);
        memberRepo.save(member);

        FoodLoyaltyTransaction txn = FoodLoyaltyTransaction.builder()
                .member(member)
                .transactionType(FoodLoyaltyTransaction.LoyaltyTransactionType.REDEEM)
                .points(points)
                .description("Redeemed " + points + " points")
                .build();

        FoodLoyaltyTransaction savedTxn = transactionRepo.save(txn);

        BigDecimal rewardValue = BigDecimal.ZERO;
        if (program.getPointValue() != null) {
            rewardValue = program.getPointValue().multiply(new BigDecimal(points));
        }

        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", savedTxn.getId());
        map.put("memberId", member.getId());
        map.put("pointsRedeemed", points);
        map.put("rewardValue", rewardValue);
        map.put("currentBalance", member.getPointsBalance());
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> validateCoupon(Long communityId, String code, BigDecimal orderAmount) {
        Map<String, Object> result = new HashMap<>();
        var couponOpt = couponRepo.findByCodeAndActive(code, true);

        if (couponOpt.isEmpty()) {
            result.put("valid", false);
            result.put("message", "Coupon not found or inactive");
            return result;
        }

        FoodLoyaltyCoupon coupon = couponOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            result.put("valid", false);
            result.put("message", "Coupon is not yet valid");
            return result;
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            result.put("valid", false);
            result.put("message", "Coupon has expired");
            return result;
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            result.put("valid", false);
            result.put("message", "Coupon usage limit reached");
            return result;
        }
        if (coupon.getMinOrder() != null && orderAmount.compareTo(coupon.getMinOrder()) < 0) {
            result.put("valid", false);
            result.put("message", "Minimum order amount is " + coupon.getMinOrder());
            return result;
        }

        BigDecimal discount;
        if (coupon.getDiscountType() == FoodLoyaltyCoupon.DiscountType.PERCENTAGE) {
            discount = orderAmount.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"));
            if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        result.put("valid", true);
        result.put("code", coupon.getCode());
        result.put("title", coupon.getTitle());
        result.put("discountType", coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null);
        result.put("discountValue", coupon.getDiscountValue());
        result.put("calculatedDiscount", discount);
        return result;
    }

    @Transactional
    public Map<String, Object> applyCoupon(Long communityId, String code, Long orderId, BigDecimal discountAmount, AppUser user) {
        var couponOpt = couponRepo.findByCodeAndActive(code, true);
        if (couponOpt.isEmpty()) {
            throw new ResourceNotFoundException("Coupon", "code", code);
        }

        FoodLoyaltyCoupon coupon = couponOpt.get();
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepo.save(coupon);

        // TODO: Save FoodLoyaltyCouponUsage when repository is available

        Map<String, Object> map = new HashMap<>();
        map.put("couponId", coupon.getId());
        map.put("code", coupon.getCode());
        map.put("orderId", orderId);
        map.put("userId", user.getId());
        map.put("discountApplied", discountAmount);
        map.put("appliedAt", LocalDateTime.now());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getGiftCards(Long communityId, Long userId) {
        // TODO: Implement when FoodLoyaltyGiftCardRepository is available
        return Collections.emptyList();
    }

    @Transactional
    public Map<String, Object> purchase(Long communityId, BigDecimal amount, Long giftedToUserId, LocalDate validUntil,
                                                 AppUser user) {
        Community community = user.getCommunity();
        // TODO: Implement when FoodLoyaltyGiftCardRepository is available
        Map<String, Object> map = new HashMap<>();
        map.put("purchasedById", user.getId());
        map.put("giftedToUserId", giftedToUserId);
        map.put("amount", amount);
        map.put("validUntil", validUntil);
        map.put("status", "ACTIVE");
        map.put("communityId", community.getId());
        map.put("createdAt", LocalDateTime.now());
        return map;
    }

    private Map<String, Object> toResponse(FoodLoyaltyProgram program) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", program.getId());
        map.put("name", program.getName());
        map.put("description", program.getDescription());
        map.put("programType", program.getProgramType() != null ? program.getProgramType().name() : null);
        map.put("pointsPerRupee", program.getPointsPerRupee());
        map.put("pointValue", program.getPointValue());
        map.put("minRedeemPoints", program.getMinRedeemPoints());
        map.put("status", program.getStatus() != null ? program.getStatus().name() : null);
        map.put("communityId", program.getCommunity() != null ? program.getCommunity().getId() : null);
        map.put("createdAt", program.getCreatedAt());
        map.put("updatedAt", program.getUpdatedAt());
        return map;
    }
}
