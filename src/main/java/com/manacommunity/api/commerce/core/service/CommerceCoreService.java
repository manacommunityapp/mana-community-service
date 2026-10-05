package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.*;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.model.CommerceRefund;
import com.manacommunity.api.user.model.AppUser;

import java.util.List;

public interface CommerceCoreService {

    List<CommerceProduct> getProducts(CommerceChannel channel);

    CommerceOrderDto checkout(AppUser buyer, CommerceCheckoutRequest request);

    List<CommerceOrderDto> getMyOrders(AppUser buyer);

    CommerceOrderDto getOrderByNumber(String orderNumber);

    HandoverVerificationResponse verifyHandover(AppUser verifier, HandoverVerificationRequest request);

    CommerceReviewDto submitReview(AppUser reviewer, CommerceReviewDto reviewDto);

    CommerceDisputeDto raiseDispute(AppUser buyer, CommerceDisputeDto disputeDto);

    CommerceRefund processRefund(AppUser user, String orderNumber, String reason);

    List<CommerceSettlementDto> getMySettlements(AppUser seller);
}
