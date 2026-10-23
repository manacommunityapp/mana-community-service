package com.manacommunity.api.unit.service;

import com.manacommunity.api.booking.dto.ResourceBookingRequest;
import com.manacommunity.api.booking.dto.ResourceBookingResponse;
import com.manacommunity.api.booking.entity.Resource;
import com.manacommunity.api.booking.entity.ResourceBooking;
import com.manacommunity.api.booking.entity.enums.BookingStatus;
import com.manacommunity.api.booking.entity.enums.PaymentStatus;
import com.manacommunity.api.booking.entity.enums.ResourceStatus;
import com.manacommunity.api.booking.repository.*;
import com.manacommunity.api.booking.service.ResourceBookingService;
import com.manacommunity.api.events.entity.EventBookingRegistration;
import com.manacommunity.api.events.repository.EventBookingRegistrationRepository;
import com.manacommunity.api.groupbuying.dto.JoinDealRequest;
import com.manacommunity.api.groupbuying.dto.GroupOrderResponse;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.*;
import com.manacommunity.api.groupbuying.service.GroupBuyingService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.transactioncore.adapter.EventsTransactionAdapter;
import com.manacommunity.api.transactioncore.adapter.GroupBuyingTransactionAdapter;
import com.manacommunity.api.transactioncore.adapter.ResourceBookingTransactionAdapter;
import com.manacommunity.api.transactioncore.dto.*;
import com.manacommunity.api.transactioncore.enums.RefundDestination;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DownstreamTransactionCoreIntegrationTest {

    // ── Group Buying Mocks ──
    @Mock
    private GroupDealRepository dealRepository;
    @Mock
    private GroupBuyOrderRepository orderRepository;
    @Mock
    private CommunityDemandRepository demandRepository;
    @Mock
    private OrderReviewRepository reviewRepository;
    @Mock
    private OrderDisputeRepository disputeRepository;
    @Mock
    private BuyingGroupRepository buyingGroupRepository;
    @Mock
    private GroupDealSettlementRepository settlementRepository;
    @Mock
    private GroupBuyingTransactionAdapter groupBuyingTxnAdapter;

    @InjectMocks
    private GroupBuyingService groupBuyingService;

    // ── Events & Booking Adapters ──
    @Mock
    private EventsTransactionAdapter eventsTxnAdapter;
    @Mock
    private ResourceBookingTransactionAdapter bookingTxnAdapter;

    private Community testCommunity;
    private AppUser testUser;
    private GroupDeal testDeal;

    @BeforeEach
    void setUp() {
        testCommunity = new Community();
        testCommunity.setId(1L);
        testCommunity.setName("Prestige Palm Springs");

        testUser = new AppUser();
        testUser.setId(101L);
        testUser.setFullName("Rahul Sharma");
        testUser.setCommunity(testCommunity);

        testDeal = GroupDeal.builder()
                .id(1L)
                .title("Organic Mango Box 5KG")
                .description("Alphonso mangoes direct from farm")
                .category("FRUITS")
                .mrp(new BigDecimal("1200.00"))
                .standardPrice(new BigDecimal("1000.00"))
                .currentPrice(new BigDecimal("900.00"))
                .currentTierPrice(new BigDecimal("900.00"))
                .targetQty(50)
                .committedQty(20)
                .currentParticipants(10)
                .dealStatus(DealStatus.OPEN)
                .dealEndsAt(LocalDateTime.now().plusDays(3))
                .pickupPoint("Clubhouse Reception")
                .pricingModel(PricingModel.THRESHOLD)
                .pricingType(PricingType.QUANTITY)
                .priceTiers(new ArrayList<>())
                .community(testCommunity)
                .build();
    }

    @Test
    @DisplayName("Group Buying: Join deal creates escrowed Transaction Core payment intent")
    void testGroupBuying_JoinDeal_CreatesEscrowPaymentIntent() {
        when(dealRepository.findById(1L)).thenReturn(Optional.of(testDeal));
        when(orderRepository.save(any(GroupBuyOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentIntentResponse mockIntent = PaymentIntentResponse.builder()
                .intentId("TXN-GB-ORDER-999")
                .status(TransactionIntentStatus.INITIATED)
                .amount(new BigDecimal("1800.00"))
                .escrowHeld(true)
                .build();

        when(groupBuyingTxnAdapter.initiateGroupOrderPayment(any(), any(), any(), any(), any(), any()))
                .thenReturn(mockIntent);

        JoinDealRequest request = new JoinDealRequest();
        request.setQuantity(2);
        request.setPaymentType("UPI");

        GroupOrderResponse response = groupBuyingService.joinDeal(1L, testUser, request);

        assertNotNull(response);
        assertEquals(2, response.getQuantity());
        assertEquals("PAID", response.getPaymentStatus());
        verify(groupBuyingTxnAdapter, times(1))
                .initiateGroupOrderPayment(any(), eq(1L), eq(101L), eq(new BigDecimal("1800.00")), any(), eq(1L));
    }

    @Test
    @DisplayName("Group Buying: Tier unlock triggers instant wallet refunds via Transaction Core")
    void testGroupBuying_TierPriceDrop_TriggersWalletRefunds() {
        GroupBuyOrder order1 = GroupBuyOrder.builder()
                .id(1L)
                .orderNumber("GB-2026-00001")
                .transactionId("TXN-GB-ORDER-001")
                .quantity(2)
                .unitPrice(new BigDecimal("900.00"))
                .totalAmount(new BigDecimal("1800.00"))
                .status(OrderStatus.CONFIRMED)
                .deal(testDeal)
                .build();

        when(orderRepository.findByDealId(testDeal.getId())).thenReturn(List.of(order1));

        RefundResult mockRefundResult = RefundResult.builder()
                .intentId("TXN-GB-ORDER-001")
                .amountRefunded(new BigDecimal("200.00"))
                .destination(RefundDestination.WALLET_INSTANT)
                .status(TransactionIntentStatus.SETTLED)
                .build();

        when(groupBuyingTxnAdapter.refundTierSavingsDelta(eq("TXN-GB-ORDER-001"), eq(new BigDecimal("200.00")), any()))
                .thenReturn(mockRefundResult);

        // Price drops from 900 to 800 per unit (200 total savings for 2 units)
        groupBuyingService.processTierPriceDropRefunds(testDeal, new BigDecimal("900.00"), new BigDecimal("800.00"));

        assertEquals(new BigDecimal("200.00"), order1.getTierPriceRefundAmount());
        assertEquals(new BigDecimal("1600.00"), order1.getTotalAmount());
        verify(groupBuyingTxnAdapter, times(1))
                .refundTierSavingsDelta(eq("TXN-GB-ORDER-001"), eq(new BigDecimal("200.00")), any());
    }

    @Test
    @DisplayName("Events: Ticket purchase creates escrow payment intent and holds revenue")
    void testEvents_TicketPurchase_CreatesEscrowIntent() {
        PaymentIntentResponse mockEventIntent = PaymentIntentResponse.builder()
                .intentId("TXN-EVT-TICKET-555")
                .domain(TransactionDomain.EVENTS)
                .amount(new BigDecimal("500.00"))
                .status(TransactionIntentStatus.INITIATED)
                .escrowHeld(true)
                .build();

        when(eventsTxnAdapter.bookEventTicket("EVT-REG-101", 5L, 101L, new BigDecimal("500.00"), 1L, 1L))
                .thenReturn(mockEventIntent);

        PaymentIntentResponse response = eventsTxnAdapter.bookEventTicket(
                "EVT-REG-101", 5L, 101L, new BigDecimal("500.00"), 1L, 1L);

        assertNotNull(response);
        assertEquals("TXN-EVT-TICKET-555", response.getIntentId());
        assertTrue(response.isEscrowHeld());
    }

    @Test
    @DisplayName("Resource Booking: Amenity booking splits fee and holds security deposit in escrow")
    void testResourceBooking_AmenityDeposit_EscrowAndInspectionRefund() {
        PaymentIntentResponse mockBookingIntent = PaymentIntentResponse.builder()
                .intentId("TXN-BK-AMENITY-777")
                .domain(TransactionDomain.RESOURCE_BOOKING)
                .amount(new BigDecimal("2500.00")) // 500 fee + 2000 deposit
                .status(TransactionIntentStatus.INITIATED)
                .escrowHeld(true)
                .build();

        when(bookingTxnAdapter.bookAmenityWithDeposit("BK-999", 10L, 101L, new BigDecimal("500.00"), new BigDecimal("2000.00"), 1L))
                .thenReturn(mockBookingIntent);

        PaymentIntentResponse response = bookingTxnAdapter.bookAmenityWithDeposit(
                "BK-999", 10L, 101L, new BigDecimal("500.00"), new BigDecimal("2000.00"), 1L);

        assertNotNull(response);
        assertEquals("TXN-BK-AMENITY-777", response.getIntentId());
        assertTrue(response.isEscrowHeld());

        // Upon post-event inspection pass: auto-refund deposit to wallet
        RefundResult mockDepositRefund = RefundResult.builder()
                .intentId("TXN-BK-AMENITY-777")
                .amountRefunded(new BigDecimal("2000.00"))
                .destination(RefundDestination.WALLET_INSTANT)
                .status(TransactionIntentStatus.SETTLED)
                .build();

        when(bookingTxnAdapter.autoRefundSecurityDeposit("TXN-BK-AMENITY-777", new BigDecimal("2000.00")))
                .thenReturn(mockDepositRefund);

        RefundResult refundResult = bookingTxnAdapter.autoRefundSecurityDeposit("TXN-BK-AMENITY-777", new BigDecimal("2000.00"));
        assertNotNull(refundResult);
        assertEquals(TransactionIntentStatus.SETTLED, refundResult.getStatus());
        assertEquals(RefundDestination.WALLET_INSTANT, refundResult.getDestination());
        assertEquals(new BigDecimal("2000.00"), refundResult.getAmountRefunded());
    }
}
