package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceFlashSaleDto;
import com.manacommunity.api.commerce.core.dto.CommerceFlashSaleRequest;
import com.manacommunity.api.commerce.core.model.CommerceFlashSale;
import com.manacommunity.api.commerce.core.model.CommerceFlashSaleItem;
import com.manacommunity.api.commerce.core.model.CommerceProduct;
import com.manacommunity.api.commerce.core.repository.CommerceFlashSaleRepository;
import com.manacommunity.api.commerce.core.repository.CommerceProductRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommerceFlashSaleService {

    private final CommerceFlashSaleRepository saleRepository;
    private final CommerceProductRepository productRepository;

    @Transactional
    public CommerceFlashSaleDto createFlashSale(AppUser admin, CommerceFlashSaleRequest req) {
        LocalDateTime start = LocalDateTime.parse(req.getStartsAt());
        LocalDateTime end = LocalDateTime.parse(req.getEndsAt());
        if (!end.isAfter(start)) throw new IllegalArgumentException("End time must be after start time");

        CommerceFlashSale sale = CommerceFlashSale.builder()
                .community(admin.getCommunity())
                .title(req.getTitle())
                .description(req.getDescription())
                .startsAt(start)
                .endsAt(end)
                .discountType(req.getDiscountType() != null ? req.getDiscountType() : "PERCENT")
                .discountValue(req.getDiscountValue())
                .maxQuantity(req.getMaxQuantity())
                .status(start.isAfter(LocalDateTime.now()) ? "SCHEDULED" : "ACTIVE")
                .createdBy(admin)
                .build();

        if (req.getProductIds() != null) {
            for (Long productId : req.getProductIds()) {
                productRepository.findById(productId).ifPresent(p -> {
                    BigDecimal flashPrice = computeFlashPrice(p, req.getDiscountType(), req.getDiscountValue());
                    sale.getItems().add(CommerceFlashSaleItem.builder()
                            .flashSale(sale)
                            .product(p)
                            .originalPrice(p.getBasePrice())
                            .flashPrice(flashPrice)
                            .quantityLimit(req.getMaxQuantity())
                            .build());
                });
            }
        }

        return toDto(saleRepository.save(sale));
    }

    @Transactional(readOnly = true)
    public List<CommerceFlashSaleDto> getActiveFlashSales(AppUser user) {
        return saleRepository.findActiveSales(user.getCommunity().getId(), LocalDateTime.now())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommerceFlashSaleDto> getAllFlashSales(AppUser admin) {
        return saleRepository.findByCommunity_IdOrderByStartsAtDesc(admin.getCommunity().getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public CommerceFlashSaleDto cancelFlashSale(AppUser admin, Long saleId) {
        CommerceFlashSale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new IllegalArgumentException("Flash sale not found: " + saleId));
        if (!sale.getCommunity().getId().equals(admin.getCommunity().getId())) {
            throw new SecurityException("Access denied");
        }
        sale.setStatus("CANCELLED");
        sale.setUpdatedAt(LocalDateTime.now());
        return toDto(saleRepository.save(sale));
    }

    /** Auto-activate scheduled sales and expire ended ones — runs every minute. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void syncSaleStatuses() {
        LocalDateTime now = LocalDateTime.now();
        // Activate
        List<CommerceFlashSale> toActivate = saleRepository.findSalesReadyToStart(now);
        toActivate.forEach(s -> { s.setStatus("ACTIVE"); s.setUpdatedAt(now); });
        if (!toActivate.isEmpty()) saleRepository.saveAll(toActivate);
        // Expire
        List<CommerceFlashSale> toExpire = saleRepository.findExpiredActiveSales(now);
        toExpire.forEach(s -> { s.setStatus("ENDED"); s.setUpdatedAt(now); });
        if (!toExpire.isEmpty()) saleRepository.saveAll(toExpire);
        if (!toActivate.isEmpty() || !toExpire.isEmpty()) {
            log.info("Flash sale sync: activated={} expired={}", toActivate.size(), toExpire.size());
        }
    }

    private BigDecimal computeFlashPrice(CommerceProduct p, String discountType, BigDecimal discountValue) {
        BigDecimal base = p.getBasePrice() != null ? p.getBasePrice() : BigDecimal.ZERO;
        if ("FLAT".equalsIgnoreCase(discountType)) {
            return base.subtract(discountValue).max(BigDecimal.ZERO);
        }
        // PERCENT
        BigDecimal discount = base.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return base.subtract(discount).max(BigDecimal.ZERO);
    }

    private CommerceFlashSaleDto toDto(CommerceFlashSale s) {
        List<CommerceFlashSaleDto.ItemDto> items = s.getItems().stream()
                .map(i -> CommerceFlashSaleDto.ItemDto.builder()
                        .id(i.getId())
                        .productId(i.getProduct().getId())
                        .productTitle(i.getProduct().getTitle())
                        .productSku(i.getProduct().getSku())
                        .thumbnailUrl(i.getProduct().getThumbnailUrl())
                        .originalPrice(i.getOriginalPrice())
                        .flashPrice(i.getFlashPrice())
                        .quantityLimit(i.getQuantityLimit())
                        .quantitySold(i.getQuantitySold())
                        .build())
                .collect(Collectors.toList());

        return CommerceFlashSaleDto.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .startsAt(s.getStartsAt() != null ? s.getStartsAt().toString() : null)
                .endsAt(s.getEndsAt() != null ? s.getEndsAt().toString() : null)
                .discountType(s.getDiscountType())
                .discountValue(s.getDiscountValue())
                .maxQuantity(s.getMaxQuantity())
                .status(s.getStatus())
                .items(items)
                .active(s.isActive())
                .build();
    }
}
