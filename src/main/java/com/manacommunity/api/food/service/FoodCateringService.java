package com.manacommunity.api.food.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.food.entity.FoodCaterer;
import com.manacommunity.api.food.entity.FoodCateringOrder;
import com.manacommunity.api.food.entity.FoodCateringPackage;
import com.manacommunity.api.food.entity.FoodCateringQuotation;
import com.manacommunity.api.food.entity.FoodCateringRequest;
import com.manacommunity.api.food.repository.FoodCatererRepository;
import com.manacommunity.api.food.repository.FoodCateringOrderRepository;
import com.manacommunity.api.food.repository.FoodCateringPackageRepository;
import com.manacommunity.api.food.repository.FoodCateringQuotationRepository;
import com.manacommunity.api.food.repository.FoodCateringRequestRepository;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FoodCateringService {

    private final FoodCatererRepository catererRepo;
    private final FoodCateringPackageRepository packageRepo;
    private final FoodCateringRequestRepository requestRepo;
    private final FoodCateringQuotationRepository quotationRepo;
    private final FoodCateringOrderRepository orderRepo;

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> list(Long communityId, Pageable pageable) {
        Page<FoodCaterer> caterers = catererRepo.findByCommunityIdAndStatus(
                communityId, FoodCaterer.CatererStatus.ACTIVE.name(), pageable);
        return caterers.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(Long communityId, Long id) {
        FoodCaterer caterer = catererRepo.findByIdAndCommunityId(id, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCaterer", id));
        return toResponse(caterer);
    }

    @Transactional
    public Map<String, Object> register(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        FoodCaterer caterer = FoodCaterer.builder()
                .name((String) request.get("name"))
                .description((String) request.get("description"))
                .cuisineTypes((String) request.get("cuisineTypes"))
                .minOrderCount(request.containsKey("minOrderCount") ? (Integer) request.get("minOrderCount") : null)
                .maxOrderCount(request.containsKey("maxOrderCount") ? (Integer) request.get("maxOrderCount") : null)
                .pricePerPlateFrom(request.containsKey("pricePerPlateFrom")
                        ? new BigDecimal(request.get("pricePerPlateFrom").toString()) : null)
                .pricePerPlateTo(request.containsKey("pricePerPlateTo")
                        ? new BigDecimal(request.get("pricePerPlateTo").toString()) : null)
                .fssaiLicense((String) request.get("fssaiLicense"))
                .logoUrl((String) request.get("logoUrl"))
                .status(FoodCaterer.CatererStatus.ACTIVE)
                .user(user)
                .community(community)
                .build();

        FoodCaterer saved = catererRepo.save(caterer);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPackages(Long communityId, Long catererId) {
        catererRepo.findByIdAndCommunityId(catererId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCaterer", catererId));
        List<FoodCateringPackage> packages = packageRepo.findByCatererIdAndActive(catererId, true);
        return packages.stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("catererId", p.getCaterer().getId());
            map.put("name", p.getName());
            map.put("description", p.getDescription());
            map.put("occasionType", p.getOccasionType() != null ? p.getOccasionType().name() : null);
            map.put("itemsPerPlate", p.getItemsPerPlate());
            map.put("pricePerPlate", p.getPricePerPlate());
            map.put("minPlates", p.getMinPlates());
            map.put("includes", p.getIncludes());
            map.put("imageUrl", p.getImageUrl());
            map.put("active", p.getActive());
            map.put("communityId", p.getCommunity().getId());
            map.put("createdAt", p.getCreatedAt());
            map.put("updatedAt", p.getUpdatedAt());
            return map;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createRequest(Long communityId, Map<String, Object> request, AppUser user) {
        Community community = user.getCommunity();
        FoodCateringRequest cateringRequest = FoodCateringRequest.builder()
                .user(user)
                .occasionType((String) request.get("occasionType"))
                .eventDate(request.containsKey("eventDate") ? LocalDate.parse((String) request.get("eventDate")) : null)
                .venue((String) request.get("venue"))
                .guestCount(request.containsKey("guestCount") ? (Integer) request.get("guestCount") : null)
                .budget(request.containsKey("budget") ? new BigDecimal(request.get("budget").toString()) : null)
                .menuPreferences((String) request.get("menuPreferences"))
                .dietaryRequirements((String) request.get("dietaryRequirements"))
                .status(FoodCateringRequest.CateringRequestStatus.OPEN)
                .community(community)
                .build();

        FoodCateringRequest saved = requestRepo.save(cateringRequest);
        return toRequestResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getMyRequests(Long communityId, Long userId, Pageable pageable) {
        Page<FoodCateringRequest> requests = requestRepo.findByUserIdAndCommunityId(userId, communityId, pageable);
        return requests.map(this::toRequestResponse);
    }

    @Transactional
    public Map<String, Object> submitQuotation(Long communityId, Map<String, Object> request) {
        Long requestId = Long.valueOf(request.get("requestId").toString());
        Long catererId = Long.valueOf(request.get("catererId").toString());
        FoodCateringRequest cateringRequest = requestRepo.findByIdAndCommunityId(requestId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCateringRequest", requestId));
        FoodCaterer caterer = catererRepo.findByIdAndCommunityId(catererId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCaterer", catererId));

        FoodCateringQuotation quotation = FoodCateringQuotation.builder()
                .request(cateringRequest)
                .caterer(caterer)
                .menu((String) request.get("menu"))
                .pricePerPlate(request.containsKey("pricePerPlate")
                        ? new BigDecimal(request.get("pricePerPlate").toString()) : null)
                .totalAmount(request.containsKey("totalAmount")
                        ? new BigDecimal(request.get("totalAmount").toString()) : null)
                .validUntil(request.containsKey("validUntil")
                        ? LocalDate.parse((String) request.get("validUntil")) : null)
                .notes((String) request.get("notes"))
                .status(FoodCateringQuotation.QuotationStatus.SUBMITTED)
                .community(cateringRequest.getCommunity())
                .build();

        FoodCateringQuotation saved = quotationRepo.save(quotation);

        cateringRequest.setStatus(FoodCateringRequest.CateringRequestStatus.QUOTED);
        requestRepo.save(cateringRequest);

        return toQuotationResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getQuotations(Long communityId, Long requestId) {
        requestRepo.findByIdAndCommunityId(requestId, communityId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCateringRequest", requestId));
        List<FoodCateringQuotation> quotations = quotationRepo.findByRequestId(requestId);
        return quotations.stream().map(this::toQuotationResponse).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> acceptQuotation(Long communityId, Long quotationId) {
        FoodCateringQuotation quotation = quotationRepo.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("FoodCateringQuotation", quotationId));

        quotation.setStatus(FoodCateringQuotation.QuotationStatus.ACCEPTED);
        quotationRepo.save(quotation);

        FoodCateringRequest cateringRequest = quotation.getRequest();
        cateringRequest.setStatus(FoodCateringRequest.CateringRequestStatus.AWARDED);
        cateringRequest.setSelectedCatererId(quotation.getCaterer().getId());
        requestRepo.save(cateringRequest);

        List<FoodCateringQuotation> otherQuotations = quotationRepo.findByRequestId(cateringRequest.getId());
        for (FoodCateringQuotation other : otherQuotations) {
            if (!other.getId().equals(quotationId)) {
                other.setStatus(FoodCateringQuotation.QuotationStatus.REJECTED);
                quotationRepo.save(other);
            }
        }

        FoodCateringOrder order = FoodCateringOrder.builder()
                .request(cateringRequest)
                .caterer(quotation.getCaterer())
                .quotation(quotation)
                .orderNumber("CO-" + System.currentTimeMillis())
                .totalAmount(quotation.getTotalAmount())
                .status(FoodCateringOrder.CateringOrderStatus.CONFIRMED)
                .community(cateringRequest.getCommunity())
                .build();

        FoodCateringOrder savedOrder = orderRepo.save(order);

        Map<String, Object> map = new HashMap<>();
        map.put("id", savedOrder.getId());
        map.put("requestId", savedOrder.getRequest().getId());
        map.put("catererId", savedOrder.getCaterer().getId());
        map.put("quotationId", savedOrder.getQuotation().getId());
        map.put("orderNumber", savedOrder.getOrderNumber());
        map.put("totalAmount", savedOrder.getTotalAmount());
        map.put("status", savedOrder.getStatus().name());
        map.put("communityId", savedOrder.getCommunity().getId());
        map.put("createdAt", savedOrder.getCreatedAt());
        map.put("updatedAt", savedOrder.getUpdatedAt());
        return map;
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getOrders(Long communityId, Pageable pageable) {
        Page<FoodCateringOrder> orders = orderRepo.findAll(pageable);
        return orders.map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", o.getId());
            map.put("requestId", o.getRequest().getId());
            map.put("catererId", o.getCaterer().getId());
            map.put("quotationId", o.getQuotation() != null ? o.getQuotation().getId() : null);
            map.put("orderNumber", o.getOrderNumber());
            map.put("totalAmount", o.getTotalAmount());
            map.put("status", o.getStatus().name());
            map.put("communityId", o.getCommunity().getId());
            map.put("createdAt", o.getCreatedAt());
            map.put("updatedAt", o.getUpdatedAt());
            return map;
        });
    }

    private Map<String, Object> toResponse(FoodCaterer caterer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", caterer.getId());
        map.put("name", caterer.getName());
        map.put("description", caterer.getDescription());
        map.put("cuisineTypes", caterer.getCuisineTypes());
        map.put("minOrderCount", caterer.getMinOrderCount());
        map.put("maxOrderCount", caterer.getMaxOrderCount());
        map.put("pricePerPlateFrom", caterer.getPricePerPlateFrom());
        map.put("pricePerPlateTo", caterer.getPricePerPlateTo());
        map.put("fssaiLicense", caterer.getFssaiLicense());
        map.put("rating", caterer.getRating());
        map.put("totalEvents", caterer.getTotalEvents());
        map.put("status", caterer.getStatus().name());
        map.put("logoUrl", caterer.getLogoUrl());
        map.put("userId", caterer.getUser().getId());
        map.put("communityId", caterer.getCommunity().getId());
        map.put("createdAt", caterer.getCreatedAt());
        map.put("updatedAt", caterer.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toRequestResponse(FoodCateringRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", request.getId());
        map.put("userId", request.getUser().getId());
        map.put("occasionType", request.getOccasionType());
        map.put("eventDate", request.getEventDate());
        map.put("venue", request.getVenue());
        map.put("guestCount", request.getGuestCount());
        map.put("budget", request.getBudget());
        map.put("menuPreferences", request.getMenuPreferences());
        map.put("dietaryRequirements", request.getDietaryRequirements());
        map.put("status", request.getStatus().name());
        map.put("selectedCatererId", request.getSelectedCatererId());
        map.put("communityId", request.getCommunity().getId());
        map.put("createdAt", request.getCreatedAt());
        map.put("updatedAt", request.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toQuotationResponse(FoodCateringQuotation quotation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", quotation.getId());
        map.put("requestId", quotation.getRequest().getId());
        map.put("catererId", quotation.getCaterer().getId());
        map.put("menu", quotation.getMenu());
        map.put("pricePerPlate", quotation.getPricePerPlate());
        map.put("totalAmount", quotation.getTotalAmount());
        map.put("validUntil", quotation.getValidUntil());
        map.put("notes", quotation.getNotes());
        map.put("status", quotation.getStatus().name());
        map.put("communityId", quotation.getCommunity().getId());
        map.put("createdAt", quotation.getCreatedAt());
        map.put("updatedAt", quotation.getUpdatedAt());
        return map;
    }
}
