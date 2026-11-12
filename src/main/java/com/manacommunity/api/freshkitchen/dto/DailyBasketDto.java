package com.manacommunity.api.freshkitchen.dto;

import com.manacommunity.api.freshkitchen.model.DailyBasket;
import com.manacommunity.api.freshkitchen.model.DailyBasketItem;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyBasketDto {

    private Long id;
    private Long userId;
    private String userName;
    private Long communityId;
    private String communityName;
    private Boolean isActive;
    private Boolean isPaused;
    private LocalDate pauseStartDate;
    private LocalDate pauseEndDate;
    private String deliverySlot;
    private String deliveryAddress;
    private String notes;

    @Builder.Default
    private List<DailyBasketItemDto> items = new ArrayList<>();

    @Builder.Default
    private BigDecimal estimatedDailyTotal = BigDecimal.ZERO;

    public static DailyBasketDto fromEntity(DailyBasket entity) {
        if (entity == null) {
            return null;
        }
        List<DailyBasketItemDto> itemDtos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        if (entity.getItems() != null) {
            for (DailyBasketItem item : entity.getItems()) {
                DailyBasketItemDto dto = DailyBasketItemDto.fromEntity(item);
                itemDtos.add(dto);
                if (Boolean.TRUE.equals(item.getIsActive()) && dto.getTotalPrice() != null) {
                    total = total.add(dto.getTotalPrice());
                }
            }
        }

        return DailyBasketDto.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userName(entity.getUser() != null ? entity.getUser().getFullName() : null)
                .communityId(entity.getCommunity() != null ? entity.getCommunity().getId() : null)
                .communityName(entity.getCommunity() != null ? entity.getCommunity().getName() : null)
                .isActive(entity.getIsActive())
                .isPaused(entity.getIsPaused())
                .pauseStartDate(entity.getPauseStartDate())
                .pauseEndDate(entity.getPauseEndDate())
                .deliverySlot(entity.getDeliverySlot())
                .deliveryAddress(entity.getDeliveryAddress())
                .notes(entity.getNotes())
                .items(itemDtos)
                .estimatedDailyTotal(total)
                .build();
    }
}
