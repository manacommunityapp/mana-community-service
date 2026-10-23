package com.manacommunity.api.service.impl;

import com.manacommunity.api.dto.PantryItemRequest;
import com.manacommunity.api.dto.PantryItemResponse;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.model.PantryItem;
import com.manacommunity.api.repository.PantryItemRepository;
import com.manacommunity.api.service.PantryService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PantryServiceImpl implements PantryService {

    private final PantryItemRepository pantryItemRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public List<PantryItemResponse> getUserPantryItems(Long userId) {
        return pantryItemRepository.findByUserIdOrderByNameAsc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PantryItemResponse getPantryItem(Long id, Long userId) {
        PantryItem item = pantryItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PantryItem", "id", id.toString()));
        return toResponse(item);
    }

    @Override
    @Transactional
    public PantryItemResponse createPantryItem(PantryItemRequest request, Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));

        PantryItem item = PantryItem.builder()
                .user(user)
                .name(request.getName())
                .category(request.getCategory())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .expiryDate(request.getExpiryDate())
                .lowStockThreshold(request.getLowStockThreshold())
                .build();

        return toResponse(pantryItemRepository.save(item));
    }

    @Override
    @Transactional
    public PantryItemResponse updatePantryItem(Long id, PantryItemRequest request, Long userId) {
        PantryItem item = pantryItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PantryItem", "id", id.toString()));

        item.setName(request.getName());
        item.setCategory(request.getCategory());
        item.setQuantity(request.getQuantity());
        item.setUnit(request.getUnit());
        item.setExpiryDate(request.getExpiryDate());
        item.setLowStockThreshold(request.getLowStockThreshold());

        return toResponse(pantryItemRepository.save(item));
    }

    @Override
    @Transactional
    public void deletePantryItem(Long id, Long userId) {
        PantryItem item = pantryItemRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PantryItem", "id", id.toString()));
        pantryItemRepository.delete(item);
    }

    @Override
    public List<PantryItemResponse> getExpiringSoonItems(Long userId) {
        LocalDate threshold = LocalDate.now().plusDays(7);
        return pantryItemRepository.findByUserIdAndExpiryDateBeforeOrderByExpiryDateAsc(userId, threshold)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private PantryItemResponse toResponse(PantryItem item) {
        return PantryItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .category(item.getCategory())
                .quantity(item.getQuantity())
                .unit(item.getUnit())
                .expiryDate(item.getExpiryDate())
                .lowStockThreshold(item.getLowStockThreshold())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
