package com.manacommunity.api.commerce.core;

import com.manacommunity.api.commerce.core.dto.CommerceCheckoutRequest;
import com.manacommunity.api.commerce.core.dto.CommerceOrderItemDto;
import com.manacommunity.api.commerce.core.dto.HandoverVerificationRequest;
import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.repository.*;
import com.manacommunity.api.commerce.core.service.CommerceCoreServiceImpl;
import com.manacommunity.api.commerce.core.service.CommerceRiskEngine;
import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommerceCoreSecurityTest")
class CommerceCoreSecurityTest {

    @Mock private CommerceOrderRepository orderRepository;
    @Mock private CommercePaymentRepository paymentRepository;
    @Mock private CommerceHandoverPassRepository handoverPassRepository;
    @Mock private CommerceReviewRepository reviewRepository;
    @Mock private CommerceDisputeRepository disputeRepository;
    @Mock private CommerceSettlementRepository settlementRepository;
    @Mock private CommerceProductRepository productRepository;
    @Mock private CommerceRefundRepository refundRepository;
    @Mock private CommerceRiskEngine riskEngine;
    @Mock private PersonalFinanceService personalFinanceService;
    @Mock private AppUserRepository userRepository;

    @InjectMocks
    private CommerceCoreServiceImpl commerceCoreService;

    private AppUser buyer;
    private AppUser seller;
    private AppUser stranger;
    private Community community;

    @BeforeEach
    void setUp() {
        community = Community.builder().id(1L).name("Mana Heights").build();
        buyer = AppUser.builder().id(101L).fullName("Alice Resident").role("USER").community(community).build();
        seller = AppUser.builder().id(202L).fullName("Bob Farmer").role("VENDOR").community(community).build();
        stranger = AppUser.builder().id(303L).fullName("Eve Attacker").role("USER").community(community).build();

        lenient().when(riskEngine.assessRisk(any(), any()))
                .thenReturn(new CommerceRiskEngine.RiskResult(0, "CLEAR", List.of()));
    }

    @Test
    @DisplayName("Should prevent price tampering and enforce database catalog price")
    void shouldPreventPriceTamperingAndEnforceCatalogPrice() {
        CommerceProduct catalogProduct = CommerceProduct.builder()
                .id(1L)
                .sku("ORGANIC-HONEY-500G")
                .title("Organic Raw Honey 500g")
                .basePrice(new BigDecimal("600.00"))
                .discountPrice(new BigDecimal("500.00"))
                .isActive(true)
                .sellerId(seller.getId())
                .build();

        when(productRepository.findBySku("ORGANIC-HONEY-500G")).thenReturn(Optional.of(catalogProduct));
        when(orderRepository.save(any(CommerceOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Attacker tampered unitPrice to 1.00 rupee in request
        CommerceCheckoutRequest request = CommerceCheckoutRequest.builder()
                .channel(CommerceChannel.MARKETPLACE)
                .fulfillmentType("CLUBHOUSE_PICKUP")
                .items(List.of(
                        CommerceOrderItemDto.builder()
                                .sku("ORGANIC-HONEY-500G")
                                .quantity(2)
                                .unitPrice(new BigDecimal("1.00")) // TAMPERED!
                                .build()
                ))
                .build();

        var orderDto = commerceCoreService.checkout(buyer, request);

        // Catalog price is 500.00 * 2 = 1000.00 subtotal, minus 5% discount = 950.00 total
        assertThat(orderDto.getSubtotalAmount()).isEqualByComparingTo("1000.00");
        assertThat(orderDto.getTotalAmount()).isEqualByComparingTo("950.00");
        assertThat(orderDto.getItems().get(0).getUnitPrice()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Should reject checkout with invalid zero or negative quantity")
    void shouldRejectCheckoutWithZeroQuantity() {
        CommerceCheckoutRequest request = CommerceCheckoutRequest.builder()
                .channel(CommerceChannel.MARKETPLACE)
                .items(List.of(
                        CommerceOrderItemDto.builder()
                                .sku("ORGANIC-HONEY-500G")
                                .quantity(0)
                                .build()
                ))
                .build();

        assertThatThrownBy(() -> commerceCoreService.checkout(buyer, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be greater than zero");
    }

    @Test
    @DisplayName("Should deny order access to unauthorized users (IDOR prevention)")
    void shouldDenyOrderAccessToUnauthorizedUser() {
        CommerceOrder order = CommerceOrder.builder()
                .id(999L)
                .orderNumber("ORD-ABC12345")
                .buyer(buyer)
                .seller(seller)
                .totalAmount(new BigDecimal("500.00"))
                .handoverOtp("654321")
                .build();

        when(orderRepository.findByOrderNumber("ORD-ABC12345")).thenReturn(Optional.of(order));

        // Stranger attempting to view Alice's order
        assertThatThrownBy(() -> commerceCoreService.getOrderByNumber(stranger, "ORD-ABC12345"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Should mask handover OTP for sellers to prevent pre-emptive verification")
    void shouldMaskHandoverOtpForSeller() {
        CommerceOrder order = CommerceOrder.builder()
                .id(999L)
                .orderNumber("ORD-ABC12345")
                .buyer(buyer)
                .seller(seller)
                .totalAmount(new BigDecimal("500.00"))
                .handoverOtp("654321")
                .build();

        when(orderRepository.findByOrderNumber("ORD-ABC12345")).thenReturn(Optional.of(order));

        // Buyer sees the OTP
        var buyerDto = commerceCoreService.getOrderByNumber(buyer, "ORD-ABC12345");
        assertThat(buyerDto.getHandoverOtp()).isEqualTo("654321");

        // Seller does NOT see the OTP
        var sellerDto = commerceCoreService.getOrderByNumber(seller, "ORD-ABC12345");
        assertThat(sellerDto.getHandoverOtp()).isNull();
    }

    @Test
    @DisplayName("Should prevent buyer from self-verifying handover")
    void shouldPreventBuyerSelfVerification() {
        CommerceOrder order = CommerceOrder.builder()
                .id(999L)
                .orderNumber("ORD-ABC12345")
                .buyer(buyer)
                .seller(seller)
                .handoverOtp("123456")
                .build();

        when(orderRepository.findByOrderNumber("ORD-ABC12345")).thenReturn(Optional.of(order));

        HandoverVerificationRequest req = HandoverVerificationRequest.builder()
                .orderNumber("ORD-ABC12345")
                .enteredOtp("123456")
                .build();

        assertThatThrownBy(() -> commerceCoreService.verifyHandover(buyer, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Buyer cannot verify their own order handover");
    }
}
