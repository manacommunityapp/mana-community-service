package com.manacommunity.api.freshkitchen.service;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.freshkitchen.dto.BasketItemRequest;
import com.manacommunity.api.freshkitchen.dto.DailyBasketDto;
import com.manacommunity.api.freshkitchen.dto.DailyBasketItemDto;
import com.manacommunity.api.freshkitchen.dto.VacationPauseRequest;
import com.manacommunity.api.freshkitchen.model.*;
import com.manacommunity.api.freshkitchen.repository.DailyBasketItemRepository;
import com.manacommunity.api.freshkitchen.repository.DailyBasketRepository;
import com.manacommunity.api.freshkitchen.repository.FreshFoodProductConfigRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyBasketService {

    private final DailyBasketRepository basketRepository;
    private final DailyBasketItemRepository basketItemRepository;
    private final FreshFoodProductConfigRepository productConfigRepository;
    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommerceOrderRepository commerceOrderRepository;
    private final VendorProductionCapacityService capacityService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public DailyBasketDto getOrCreateUserBasket(Long userId, Long communityId) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseGet(() -> {
                    AppUser user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("AppUser", userId));
                    Community community = communityRepository.findById(communityId)
                            .orElseThrow(() -> new ResourceNotFoundException("Community", communityId));

                    String defaultAddress = (user.getFlatNo() != null ? "Flat " + user.getFlatNo() : "")
                            + (user.getTower() != null ? ", Tower " + user.getTower() : "")
                            + (user.getBlock() != null ? ", Block " + user.getBlock() : "");
                    if (defaultAddress.isBlank()) {
                        defaultAddress = "Community Resident Doorstep";
                    }

                    DailyBasket newBasket = DailyBasket.builder()
                            .user(user)
                            .community(community)
                            .isActive(true)
                            .isPaused(false)
                            .deliverySlot("MORNING_06_00_07_00")
                            .deliveryAddress(defaultAddress)
                            .build();
                    return basketRepository.save(newBasket);
                });

        return DailyBasketDto.fromEntity(basket);
    }

    @Transactional
    public DailyBasketDto updateBasketConfig(Long userId, Long communityId, DailyBasketDto dto) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasket for user " + userId + " and community " + communityId));

        if (dto.getDeliverySlot() != null) {
            basket.setDeliverySlot(dto.getDeliverySlot());
        }
        if (dto.getDeliveryAddress() != null) {
            basket.setDeliveryAddress(dto.getDeliveryAddress());
        }
        if (dto.getNotes() != null) {
            basket.setNotes(dto.getNotes());
        }
        if (dto.getIsActive() != null) {
            basket.setIsActive(dto.getIsActive());
        }

        basket = basketRepository.save(basket);
        return DailyBasketDto.fromEntity(basket);
    }

    @Transactional
    public DailyBasketItemDto addItem(Long userId, Long communityId, BasketItemRequest request) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseGet(() -> {
                    getOrCreateUserBasket(userId, communityId);
                    return basketRepository.findByUserIdAndCommunityId(userId, communityId).orElseThrow();
                });

        FreshFoodProductConfig config = productConfigRepository.findByProductId(request.getProductId())
                .orElse(null);

        String productName = config != null ? config.getName() : request.getProductName();
        if (productName == null || productName.isBlank()) {
            productName = request.getProductId();
        }

        BigDecimal unitPrice = config != null ? config.getUnitPrice() : (request.getUnitPrice() != null ? request.getUnitPrice() : BigDecimal.ZERO);
        FoodCategory category = config != null ? config.getCategory() : (request.getCategory() != null ? request.getCategory() : FoodCategory.BATTER);
        ExecutionMode mode = config != null ? config.getExecutionMode() : (request.getExecutionMode() != null ? request.getExecutionMode() : ExecutionMode.DAILY_BASKET);

        DailyBasketItem item = DailyBasketItem.builder()
                .basket(basket)
                .productId(request.getProductId())
                .productName(productName)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .unitPrice(unitPrice)
                .category(category)
                .executionMode(mode)
                .mon(request.getMon() != null ? request.getMon() : true)
                .tue(request.getTue() != null ? request.getTue() : true)
                .wed(request.getWed() != null ? request.getWed() : true)
                .thu(request.getThu() != null ? request.getThu() : true)
                .fri(request.getFri() != null ? request.getFri() : true)
                .sat(request.getSat() != null ? request.getSat() : true)
                .sun(request.getSun() != null ? request.getSun() : true)
                .specialInstructions(request.getSpecialInstructions())
                .isActive(true)
                .build();

        item.syncDaysOfWeekString();
        item = basketItemRepository.save(item);
        return DailyBasketItemDto.fromEntity(item);
    }

    @Transactional
    public DailyBasketItemDto updateItem(Long userId, Long communityId, Long itemId, BasketItemRequest request) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasket for user " + userId));

        DailyBasketItem item = basketItemRepository.findByIdAndBasketId(itemId, basket.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasketItem", itemId));

        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getMon() != null) item.setMon(request.getMon());
        if (request.getTue() != null) item.setTue(request.getTue());
        if (request.getWed() != null) item.setWed(request.getWed());
        if (request.getThu() != null) item.setThu(request.getThu());
        if (request.getFri() != null) item.setFri(request.getFri());
        if (request.getSat() != null) item.setSat(request.getSat());
        if (request.getSun() != null) item.setSun(request.getSun());
        if (request.getSpecialInstructions() != null) item.setSpecialInstructions(request.getSpecialInstructions());

        item.syncDaysOfWeekString();
        item = basketItemRepository.save(item);
        return DailyBasketItemDto.fromEntity(item);
    }

    @Transactional
    public void removeItem(Long userId, Long communityId, Long itemId) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasket for user " + userId));

        DailyBasketItem item = basketItemRepository.findByIdAndBasketId(itemId, basket.getId())
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasketItem", itemId));

        basketItemRepository.delete(item);
    }

    @Transactional
    public DailyBasketDto pauseBasket(Long userId, Long communityId, VacationPauseRequest request) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasket for user " + userId));

        if (request.getPauseStartDate() == null || request.getPauseEndDate() == null) {
            throw new InvalidInputException("Both pauseStartDate and pauseEndDate are required");
        }
        if (request.getPauseEndDate().isBefore(request.getPauseStartDate())) {
            throw new InvalidInputException("pauseEndDate cannot be before pauseStartDate");
        }

        basket.setIsPaused(true);
        basket.setPauseStartDate(request.getPauseStartDate());
        basket.setPauseEndDate(request.getPauseEndDate());
        basket = basketRepository.save(basket);

        log.info("Daily basket {} paused from {} to {} for user {}", basket.getId(),
                request.getPauseStartDate(), request.getPauseEndDate(), userId);
        return DailyBasketDto.fromEntity(basket);
    }

    @Transactional
    public DailyBasketDto resumeBasket(Long userId, Long communityId) {
        DailyBasket basket = basketRepository.findByUserIdAndCommunityId(userId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("DailyBasket for user " + userId));

        basket.setIsPaused(false);
        basket.setPauseStartDate(null);
        basket.setPauseEndDate(null);
        basket = basketRepository.save(basket);

        log.info("Daily basket {} resumed for user {}", basket.getId(), userId);
        return DailyBasketDto.fromEntity(basket);
    }

    /**
     * 10:00 PM Nightly Engine:
     * Generates authoritative CommerceOrder rows for active baskets eligible on targetDate.
     * Uses deterministic occurrence key `DB-YYYYMMDD-{basketId}` to ensure idempotent once-and-only-once execution.
     */
    @Transactional
    public int generateDailyOrders(LocalDate targetDate) {
        LocalDate date = targetDate != null ? targetDate : LocalDate.now().plusDays(1);
        DayOfWeek dow = date.getDayOfWeek();
        String dateFormatted = date.format(DateTimeFormatter.BASIC_ISO_DATE);

        List<DailyBasket> baskets = basketRepository.findByIsActiveTrue();
        int ordersGenerated = 0;

        for (DailyBasket basket : baskets) {
            if (basket.isPausedOn(date)) {
                log.debug("Basket {} is paused on date {}. Skipping order generation.", basket.getId(), date);
                continue;
            }

            List<DailyBasketItem> eligibleItems = basket.getItems().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIsActive()))
                    .filter(item -> item.isActiveOnDay(dow))
                    .toList();

            if (eligibleItems.isEmpty()) {
                continue;
            }

            // Deterministic order number for idempotent duplicate protection
            String orderNum = "DB-" + dateFormatted + "-" + basket.getId();
            if (commerceOrderRepository.findByOrderNumber(orderNum).isPresent()) {
                log.debug("Order {} for basket {} on {} already generated. Skipping.", orderNum, basket.getId(), date);
                continue;
            }

            BigDecimal subtotal = BigDecimal.ZERO;
            List<CommerceOrderItem> orderItems = new ArrayList<>();

            CommerceOrder order = CommerceOrder.builder()
                    .orderNumber(orderNum)
                    .channel(CommerceChannel.FOOD_DELIVERY)
                    .buyer(basket.getUser())
                    .community(basket.getCommunity())
                    .vendorId("VND-FRESH-KITCHEN")
                    .vendorName("Mana Fresh Kitchen")
                    .channelReferenceId(String.valueOf(basket.getId()))
                    .status(CommerceOrderStatus.CONFIRMED)
                    .subtotalAmount(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .savingsAmount(BigDecimal.ZERO)
                    .paymentMethod("CASHFREE_SUBSCRIPTION")
                    .paymentStatus("INITIATED")
                    .fulfillmentType("DOORSTEP")
                    .deliveryAddress(basket.getDeliveryAddress() != null ? basket.getDeliveryAddress() : "Community Doorstep")
                    .pickupSlot(basket.getDeliverySlot())
                    .estimatedDeliveryDate(date)
                    .handoverOtp(String.format("%06d", secureRandom.nextInt(900000) + 100000))
                    .qrToken("TKN-DB-" + basket.getId() + "-" + dateFormatted + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .isEscrowLocked(true)
                    .build();

            for (DailyBasketItem item : eligibleItems) {
                // Check and reserve vendor production capacity
                capacityService.checkAndReserveAnyCapacity(item.getProductId(), date, item.getQuantity());

                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
                subtotal = subtotal.add(lineTotal);

                CommerceOrderItem orderItem = CommerceOrderItem.builder()
                        .order(order)
                        .productId(item.getProductId())
                        .sku("SKU-" + item.getProductId())
                        .title(item.getProductName())
                        .unitPrice(unitPrice)
                        .quantity(item.getQuantity())
                        .totalPrice(lineTotal)
                        .build();

                orderItems.add(orderItem);
            }

            order.setSubtotalAmount(subtotal);
            order.setTotalAmount(subtotal);
            order.setItems(orderItems);

            commerceOrderRepository.save(order);
            ordersGenerated++;
            log.info("Generated Daily Basket CommerceOrder {} for basket {} total ₹{}", orderNum, basket.getId(), subtotal);
        }

        return ordersGenerated;
    }
}
