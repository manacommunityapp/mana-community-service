package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodCommunityKitchen;
import com.manacommunity.api.food.entity.FoodCommunityKitchenBooking;
import com.manacommunity.api.food.entity.FoodCommunityKitchenMenu;
import com.manacommunity.api.food.entity.FoodCommunityKitchenToken;
import com.manacommunity.api.food.repository.FoodCommunityKitchenBookingRepository;
import com.manacommunity.api.food.repository.FoodCommunityKitchenMenuRepository;
import com.manacommunity.api.food.repository.FoodCommunityKitchenRepository;
import com.manacommunity.api.food.repository.FoodCommunityKitchenTokenRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodCommunityKitchenService {

    private final FoodCommunityKitchenRepository kitchenRepo;
    private final FoodCommunityKitchenMenuRepository menuRepo;
    private final FoodCommunityKitchenBookingRepository bookingRepo;
    private final FoodCommunityKitchenTokenRepository tokenRepo;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getKitchens(Long communityId) {
        List<FoodCommunityKitchen> kitchens = kitchenRepo.findByCommunityId(communityId);
        return kitchens.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getKitchenById(Long id, Long communityId) {
        FoodCommunityKitchen kitchen = kitchenRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCommunityKitchen", id));
        return toResponse(kitchen);
    }

    @Transactional
    public Map<String, Object> createKitchen(Map<String, Object> request, AppUser user, Community community) {
        FoodCommunityKitchen kitchen = FoodCommunityKitchen.builder()
                .name((String) request.get("name"))
                .kitchenType(FoodCommunityKitchen.CommunityKitchenType.valueOf((String) request.get("kitchenType")))
                .location((String) request.get("location"))
                .description((String) request.get("description"))
                .capacity(request.containsKey("capacity") ? (Integer) request.get("capacity") : null)
                .manager(user)
                .status(FoodCommunityKitchen.CommunityKitchenStatus.ACTIVE)
                .openingTime(request.containsKey("openingTime") ? LocalTime.parse((String) request.get("openingTime")) : null)
                .closingTime(request.containsKey("closingTime") ? LocalTime.parse((String) request.get("closingTime")) : null)
                .community(community)
                .build();

        FoodCommunityKitchen saved = kitchenRepo.save(kitchen);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMenu(Long kitchenId, LocalDate date) {
        List<FoodCommunityKitchenMenu> menus = menuRepo.findByKitchenIdAndDate(kitchenId, date);
        return menus.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId());
            map.put("kitchenId", m.getKitchen().getId());
            map.put("date", m.getDate());
            map.put("mealType", m.getMealType().name());
            map.put("items", m.getItems());
            map.put("pricePerPlate", m.getPricePerPlate());
            map.put("totalPlates", m.getTotalPlates());
            map.put("bookedPlates", m.getBookedPlates());
            map.put("cutoffTime", m.getCutoffTime());
            map.put("communityId", m.getCommunity().getId());
            map.put("createdAt", m.getCreatedAt());
            map.put("updatedAt", m.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createMenu(Long kitchenId, Map<String, Object> request, Long communityId) {
        FoodCommunityKitchen kitchen = kitchenRepo.findByIdAndCommunityId(kitchenId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCommunityKitchen", kitchenId));

        FoodCommunityKitchenMenu menu = FoodCommunityKitchenMenu.builder()
                .kitchen(kitchen)
                .date(LocalDate.parse((String) request.get("date")))
                .mealType(FoodCommunityKitchenMenu.MealType.valueOf((String) request.get("mealType")))
                .items((String) request.get("items"))
                .pricePerPlate(new BigDecimal(request.get("pricePerPlate").toString()))
                .totalPlates((Integer) request.get("totalPlates"))
                .bookedPlates(0)
                .cutoffTime(request.containsKey("cutoffTime") ? LocalDateTime.parse((String) request.get("cutoffTime")) : null)
                .community(kitchen.getCommunity())
                .build();

        FoodCommunityKitchenMenu saved = menuRepo.save(menu);

        Map<String, Object> map = new HashMap<>();
        map.put("id", saved.getId());
        map.put("kitchenId", saved.getKitchen().getId());
        map.put("date", saved.getDate());
        map.put("mealType", saved.getMealType().name());
        map.put("items", saved.getItems());
        map.put("pricePerPlate", saved.getPricePerPlate());
        map.put("totalPlates", saved.getTotalPlates());
        map.put("bookedPlates", saved.getBookedPlates());
        map.put("cutoffTime", saved.getCutoffTime());
        map.put("communityId", saved.getCommunity().getId());
        map.put("createdAt", saved.getCreatedAt());
        map.put("updatedAt", saved.getUpdatedAt());
        return map;
    }

    @Transactional
    public Map<String, Object> bookMeal(Long menuId, Integer quantity, AppUser user, Community community) {
        FoodCommunityKitchenMenu menu = menuRepo.findById(menuId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCommunityKitchenMenu", menuId));

        BigDecimal totalAmount = menu.getPricePerPlate().multiply(BigDecimal.valueOf(quantity));
        String pickupCode = String.format("%06d", new java.util.Random().nextInt(999999));

        FoodCommunityKitchenBooking booking = FoodCommunityKitchenBooking.builder()
                .menu(menu)
                .user(user)
                .quantity(quantity)
                .totalAmount(totalAmount)
                .status(FoodCommunityKitchenBooking.BookingStatus.BOOKED)
                .pickupCode(pickupCode)
                .community(community)
                .build();

        FoodCommunityKitchenBooking savedBooking = bookingRepo.save(booking);

        menu.setBookedPlates(menu.getBookedPlates() + quantity);
        menuRepo.save(menu);

        String qrCode = UUID.randomUUID().toString();
        String tokenNumber = "TKN-" + savedBooking.getId() + "-" + System.currentTimeMillis();

        FoodCommunityKitchenToken token = FoodCommunityKitchenToken.builder()
                .booking(savedBooking)
                .tokenNumber(tokenNumber)
                .qrCode(qrCode)
                .status(FoodCommunityKitchenToken.TokenStatus.VALID)
                .build();

        tokenRepo.save(token);

        Map<String, Object> map = new HashMap<>();
        map.put("id", savedBooking.getId());
        map.put("menuId", savedBooking.getMenu().getId());
        map.put("userId", savedBooking.getUser().getId());
        map.put("quantity", savedBooking.getQuantity());
        map.put("totalAmount", savedBooking.getTotalAmount());
        map.put("status", savedBooking.getStatus().name());
        map.put("pickupCode", savedBooking.getPickupCode());
        map.put("pickupTime", savedBooking.getPickupTime());
        map.put("tokenNumber", tokenNumber);
        map.put("qrCode", qrCode);
        map.put("communityId", savedBooking.getCommunity().getId());
        map.put("createdAt", savedBooking.getCreatedAt());
        map.put("updatedAt", savedBooking.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyBookings(Long userId, Long communityId, Pageable pageable) {
        Page<FoodCommunityKitchenBooking> bookings = bookingRepo.findByUserIdAndCommunityId(userId, communityId, pageable);
        return bookings.map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("menuId", b.getMenu().getId());
            map.put("userId", b.getUser().getId());
            map.put("quantity", b.getQuantity());
            map.put("totalAmount", b.getTotalAmount());
            map.put("status", b.getStatus().name());
            map.put("pickupCode", b.getPickupCode());
            map.put("pickupTime", b.getPickupTime());
            map.put("pickedUpAt", b.getPickedUpAt());
            map.put("communityId", b.getCommunity().getId());
            map.put("createdAt", b.getCreatedAt());
            map.put("updatedAt", b.getUpdatedAt());
            return map;
        });
    }

    @Transactional
    public Map<String, Object> verifyToken(String qrCode) {
        FoodCommunityKitchenToken token = tokenRepo.findByQrCode(qrCode)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCommunityKitchenToken", "qrCode", qrCode));

        token.setStatus(FoodCommunityKitchenToken.TokenStatus.USED);
        token.setUsedAt(LocalDateTime.now());
        tokenRepo.save(token);

        Map<String, Object> map = new HashMap<>();
        map.put("id", token.getId());
        map.put("bookingId", token.getBooking().getId());
        map.put("tokenNumber", token.getTokenNumber());
        map.put("qrCode", token.getQrCode());
        map.put("status", token.getStatus().name());
        map.put("usedAt", token.getUsedAt());
        map.put("createdAt", token.getCreatedAt());
        return map;
    }

    private Map<String, Object> toResponse(FoodCommunityKitchen kitchen) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", kitchen.getId());
        map.put("name", kitchen.getName());
        map.put("kitchenType", kitchen.getKitchenType().name());
        map.put("location", kitchen.getLocation());
        map.put("description", kitchen.getDescription());
        map.put("capacity", kitchen.getCapacity());
        map.put("managerId", kitchen.getManager() != null ? kitchen.getManager().getId() : null);
        map.put("status", kitchen.getStatus().name());
        map.put("openingTime", kitchen.getOpeningTime());
        map.put("closingTime", kitchen.getClosingTime());
        map.put("communityId", kitchen.getCommunity().getId());
        map.put("createdAt", kitchen.getCreatedAt());
        map.put("updatedAt", kitchen.getUpdatedAt());
        return map;
    }
}
