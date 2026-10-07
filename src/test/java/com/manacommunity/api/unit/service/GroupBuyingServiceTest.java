package com.manacommunity.api.unit.service;

import com.manacommunity.api.groupbuying.dto.*;
import com.manacommunity.api.groupbuying.model.*;
import com.manacommunity.api.groupbuying.repository.*;
import com.manacommunity.api.groupbuying.service.GroupBuyingService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupBuyingServiceTest {

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

    @InjectMocks
    private GroupBuyingService groupBuyingService;

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
                .title("Aashirvaad Atta 10 KG")
                .description("Whole wheat flour")
                .category("GROCERY")
                .mrp(new BigDecimal("680.00"))
                .standardPrice(new BigDecimal("640.00"))
                .currentPrice(new BigDecimal("585.00"))
                .currentTierPrice(new BigDecimal("585.00"))
                .targetQty(100)
                .committedQty(73)
                .currentParticipants(45)
                .dealStatus(DealStatus.OPEN)
                .dealEndsAt(LocalDateTime.now().plusDays(3))
                .pickupPoint("Clubhouse Ground Floor")
                .pickupDate(LocalDateTime.now().plusDays(5))
                .vendor("ITC Wholesale Direct")
                .vendorRating(4.8)
                .vendorVerified(true)
                .pricingModel(PricingModel.THRESHOLD)
                .pricingType(PricingType.QUANTITY)
                .priceTiers(new ArrayList<>())
                .community(testCommunity)
                .build();
    }

    @Test
    void testGetDeals() {
        when(dealRepository.findByCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(testDeal));

        List<GroupDealResponse> deals = groupBuyingService.getDeals(1L);

        assertNotNull(deals);
        assertEquals(1, deals.size());
        assertEquals("Aashirvaad Atta 10 KG", deals.get(0).getTitle());
    }

    @Test
    void testJoinDealSuccess() {
        when(dealRepository.findById(1L)).thenReturn(Optional.of(testDeal));
        when(orderRepository.save(any(GroupBuyOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JoinDealRequest request = new JoinDealRequest();
        request.setQuantity(2);

        GroupOrderResponse order = groupBuyingService.joinDeal(1L, testUser, request);

        assertNotNull(order);
        assertEquals(2, order.getQuantity());
        assertEquals(new BigDecimal("1170.00"), order.getTotalAmount());
        assertEquals(OrderStatus.CONFIRMED.name(), order.getStatus());
        assertNotNull(order.getQrToken());
        assertEquals(75, testDeal.getCommittedQty());
    }

    @Test
    void testCreateDemand() {
        CommunityDemand demand = CommunityDemand.builder()
                .title("Organic Ghee 1L")
                .category("DAIRY")
                .description("Farm fresh A2 ghee")
                .expectedQty(1)
                .interestedResidents(1)
                .upvotesCount(1)
                .status(DemandStatus.OPEN)
                .vendorOffers(new ArrayList<>())
                .community(testCommunity)
                .createdByUser(testUser)
                .build();

        when(demandRepository.save(any(CommunityDemand.class))).thenReturn(demand);

        CreateDemandRequest request = new CreateDemandRequest();
        request.setTitle("Organic Ghee 1L");
        request.setCategory("DAIRY");
        request.setDescription("Farm fresh A2 ghee");
        request.setPreferredPriceMin(new BigDecimal("800.00"));
        request.setPreferredPriceMax(new BigDecimal("950.00"));
        request.setExpectedQty(1);

        DemandResponse response = groupBuyingService.createDemand(1L, testUser, request);

        assertNotNull(response);
        assertEquals("Organic Ghee 1L", response.getTitle());
        assertEquals(1, response.getInterestedResidents());
    }

    @Test
    void testCommunityBuyingPowerMetrics() {
        when(orderRepository.findByCommunityIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

        CommunitySavingsResponse savings = groupBuyingService.getCommunitySavings(1L);

        assertNotNull(savings);
        assertEquals(BigDecimal.valueOf(184520), savings.getTotalSavedThisMonth());
        assertEquals(1248, savings.getTotalOrders());
        assertEquals(38, savings.getActiveDeals());
        assertEquals(2450, savings.getTotalKgsBought());
        assertEquals(BigDecimal.valueOf(147), savings.getAvgSavingPerOrder());
        assertEquals(23.8, savings.getCollectiveDiscountPercent());
        assertTrue(savings.getHeroMilestoneText().contains("₹18.4 lakh"));
        assertNotNull(savings.getTopDealsThisMonth());
        assertEquals(3, savings.getTopDealsThisMonth().size());
        assertNotNull(savings.getTowerLeaderboard());
        assertEquals(3, savings.getTowerLeaderboard().size());
    }

    @Test
    void testVerifyPickupPass() {
        GroupBuyOrder order = GroupBuyOrder.builder()
                .deal(testDeal)
                .dealTitle("Aashirvaad Atta 10 KG")
                .quantity(2)
                .unitPrice(new BigDecimal("585.00"))
                .totalAmount(new BigDecimal("1170.00"))
                .status(OrderStatus.CONFIRMED)
                .qrToken("QR-TEST-123")
                .build();

        when(orderRepository.findByQrToken("QR-TEST-123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(GroupBuyOrder.class))).thenAnswer(i -> i.getArgument(0));

        PickupVerificationRequest request = new PickupVerificationRequest();
        request.setQrToken("QR-TEST-123");

        PickupVerificationResponse response = groupBuyingService.verifyPickupPass(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(OrderStatus.PICKED_UP.name(), response.getOrder().getStatus());
    }
}
