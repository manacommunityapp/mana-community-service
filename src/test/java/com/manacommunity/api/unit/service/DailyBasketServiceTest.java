package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommerceWalletRepository;
import com.manacommunity.api.commerce.core.repository.CommerceWalletTransactionRepository;
import com.manacommunity.api.commerce.core.service.CommerceNotificationService;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.freshkitchen.dto.DeliveryPassDto;
import com.manacommunity.api.freshkitchen.dto.HandoverVerifyRequest;
import com.manacommunity.api.freshkitchen.model.DailyBasket;
import com.manacommunity.api.freshkitchen.model.DailyBasketItem;
import com.manacommunity.api.freshkitchen.repository.DailyBasketItemRepository;
import com.manacommunity.api.freshkitchen.repository.DailyBasketRepository;
import com.manacommunity.api.freshkitchen.repository.FreshFoodProductConfigRepository;
import com.manacommunity.api.freshkitchen.service.DailyBasketService;
import com.manacommunity.api.freshkitchen.service.VendorProductionCapacityService;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyBasketServiceTest {

    @Mock
    private DailyBasketRepository basketRepository;
    @Mock
    private DailyBasketItemRepository basketItemRepository;
    @Mock
    private FreshFoodProductConfigRepository productConfigRepository;
    @Mock
    private AppUserRepository userRepository;
    @Mock
    private CommunityRepository communityRepository;
    @Mock
    private CommerceOrderRepository commerceOrderRepository;
    @Mock
    private CommerceWalletRepository commerceWalletRepository;
    @Mock
    private CommerceWalletTransactionRepository commerceWalletTransactionRepository;
    @Mock
    private VendorProductionCapacityService capacityService;
    @Mock
    private CommerceNotificationService notificationService;

    @InjectMocks
    private DailyBasketService dailyBasketService;

    private AppUser testUser;
    private CommerceOrder testOrder;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setId(101L);
        testUser.setFullName("Priya Sharma");
        testUser.setFlatNo("402");
        testUser.setTower("B");

        testOrder = CommerceOrder.builder()
                .id(9021L)
                .orderNumber("DB-20261104-1")
                .buyer(testUser)
                .status(CommerceOrderStatus.CONFIRMED)
                .handoverOtp("481923")
                .qrToken("TKN-DB-1-20261104-ABCD1234")
                .deliveryAddress("Flat 402, Tower B, Mana Community")
                .pickupSlot("06:00 AM - 07:30 AM")
                .estimatedDeliveryDate(LocalDate.now())
                .isEscrowLocked(true)
                .totalAmount(new BigDecimal("175.00"))
                .paymentStatus("PAID")
                .paymentMethod("COMMERCE_WALLET")
                .items(new ArrayList<>())
                .build();
    }

    @Test
    void testVerifyDeliveryHandover_ValidOtp_MarksDeliveredAndUnlocksEscrow() {
        when(commerceOrderRepository.findByOrderNumber("DB-20261104-1"))
                .thenReturn(Optional.of(testOrder));
        when(commerceOrderRepository.save(any(CommerceOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        HandoverVerifyRequest request = HandoverVerifyRequest.builder()
                .orderNumber("DB-20261104-1")
                .handoverOtp("481923")
                .runnerNotes("Handed directly to resident")
                .build();

        DeliveryPassDto result = dailyBasketService.verifyDeliveryHandover(request);

        assertNotNull(result);
        assertEquals("DELIVERED", result.getStatus());
        assertFalse(Boolean.TRUE.equals(result.getIsEscrowLocked()), "Escrow should be unlocked upon verified handover");
        assertNotNull(result.getActualDeliveryAt(), "Actual delivery timestamp should be recorded");

        verify(commerceOrderRepository).save(testOrder);
        verify(notificationService).notifyMorningHandoverVerified(testOrder);
    }

    @Test
    void testVerifyDeliveryHandover_InvalidOtp_ThrowsException() {
        when(commerceOrderRepository.findByOrderNumber("DB-20261104-1"))
                .thenReturn(Optional.of(testOrder));

        HandoverVerifyRequest request = HandoverVerifyRequest.builder()
                .orderNumber("DB-20261104-1")
                .handoverOtp("999999") // Incorrect OTP
                .build();

        assertThrows(InvalidInputException.class, () ->
                dailyBasketService.verifyDeliveryHandover(request)
        );

        verify(commerceOrderRepository, never()).save(any());
        verify(notificationService, never()).notifyMorningHandoverVerified(any());
    }

    @Test
    void testLookupOrderByTokenOrNumber_FindsByQrToken() {
        when(commerceOrderRepository.findByQrToken("TKN-DB-1-20261104-ABCD1234"))
                .thenReturn(Optional.of(testOrder));

        DeliveryPassDto pass = dailyBasketService.lookupOrderByTokenOrNumber("TKN-DB-1-20261104-ABCD1234");

        assertNotNull(pass);
        assertEquals("DB-20261104-1", pass.getOrderNumber());
        assertEquals("481923", pass.getHandoverOtp());
        assertEquals("402", pass.getDeliveryAddress().contains("402") ? "402" : "");
    }

    @Test
    void testSendNightlyCutoffReminders_SendsPushToActiveBaskets() {
        DailyBasket basket = new DailyBasket();
        basket.setId(1L);
        basket.setUser(testUser);
        basket.setIsActive(true);
        basket.setDeliverySlot("06:00 AM - 07:30 AM");

        DailyBasketItem item = new DailyBasketItem();
        item.setId(10L);
        item.setIsActive(true);
        item.setMon(true);
        item.setTue(true);
        item.setWed(true);
        item.setThu(true);
        item.setFri(true);
        item.setSat(true);
        item.setSun(true);
        basket.setItems(List.of(item));

        when(basketRepository.findByIsActiveTrue()).thenReturn(List.of(basket));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        dailyBasketService.sendNightlyCutoffReminders(tomorrow);

        verify(notificationService).notifyNightlyBasketCutoffReminder(eq(101L), eq("06:00 AM - 07:30 AM"));
    }

    @Test
    void testDispatchMorningOrders_TransitionsToOutForDeliveryAndSendsPin() {
        LocalDate today = LocalDate.now();
        when(commerceOrderRepository.findAll()).thenReturn(List.of(testOrder));
        when(commerceOrderRepository.save(any(CommerceOrder.class))).thenAnswer(inv -> inv.getArgument(0));

        int dispatched = dailyBasketService.dispatchMorningOrders(today);

        assertEquals(1, dispatched);
        assertEquals(CommerceOrderStatus.OUT_FOR_DELIVERY, testOrder.getStatus());
        verify(notificationService).notifyMorningOutForDeliveryWithPin(testOrder);
    }
}
