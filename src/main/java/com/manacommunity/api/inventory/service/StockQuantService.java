package com.manacommunity.api.inventory.service;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.entity.*;
import com.manacommunity.api.inventory.exception.InsufficientStockException;
import com.manacommunity.api.inventory.mapper.InventoryMapper;
import com.manacommunity.api.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockQuantService {

    private final StockQuantRepository quantRepository;
    private final StockLotRepository lotRepository;
    private final ProductRepository productRepository;
    private final StockLocationRepository locationRepository;

    @Transactional(readOnly = true)
    public List<QuantDto> getAllQuants() {
        return quantRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LotDto> getAllLots() {
        return lotRepository.findAll().stream()
                .map(l -> {
                    String pName = productRepository.findById(l.getProductId())
                            .map(Product::getName)
                            .orElse("Unknown Product");
                    return InventoryMapper.toDto(l, pName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public LotDto createLot(LotDto dto) {
        StockLot lot = StockLot.builder()
                .name(dto.getName())
                .productId(dto.getProductId())
                .productQty(0.0)
                .build();
        lot = lotRepository.save(lot);
        String pName = productRepository.findById(lot.getProductId())
                .map(Product::getName)
                .orElse("Unknown Product");
        return InventoryMapper.toDto(lot, pName);
    }

    @Transactional
    public void adjustStock(Long productId, Long locationId, Long lotId, Double deltaQuantity) {
        // Fetch quant for update using Pessimistic Write Lock
        Optional<StockQuant> optQuant = quantRepository.findForUpdate(productId, locationId, lotId);
        StockQuant quant;
        if (optQuant.isPresent()) {
            quant = optQuant.get();
            double newQty = quant.getQuantity() + deltaQuantity;
            if (newQty < 0) {
                throw new InsufficientStockException("Cannot adjust stock. Resulting quantity would be negative: " + newQty);
            }
            quant.setQuantity(newQty);
        } else {
            if (deltaQuantity < 0) {
                throw new InsufficientStockException("Cannot adjust stock. No stock exists at this location to decrease.");
            }
            quant = StockQuant.builder()
                    .productId(productId)
                    .locationId(locationId)
                    .lotId(lotId)
                    .quantity(deltaQuantity)
                    .reservedQuantity(0.0)
                    .inDate(LocalDateTime.now())
                    .build();
        }
        quantRepository.save(quant);

        // Update lot quantities if tracked by lot
        if (lotId != null) {
            lotRepository.findById(lotId).ifPresent(l -> {
                double newLotQty = l.getProductQty() + deltaQuantity;
                l.setProductQty(Math.max(0.0, newLotQty));
                lotRepository.save(l);
            });
        }
    }

    @Transactional
    public void scrapStock(Long productId, Long locationId, Long lotId, Double scrapQuantity, String reason) {
        // Scrapping moves stock from source location to Virtual/Scrap location
        StockLocation scrapLoc = locationRepository.findByCompleteName("Virtual/Scrap")
                .orElseGet(() -> locationRepository.save(StockLocation.builder()
                        .completeName("Virtual/Scrap")
                        .usage(LocationUsage.scrap)
                        .barcode("SCRAP")
                        .build()));

        // Subtract from source location
        adjustStock(productId, locationId, lotId, -scrapQuantity);

        // Add to scrap location
        adjustStock(productId, scrapLoc.getId(), lotId, scrapQuantity);
    }

    private QuantDto mapToDto(StockQuant q) {
        String pName = productRepository.findById(q.getProductId())
                .map(Product::getName)
                .orElse("Unknown Product");
        String pSku = productRepository.findById(q.getProductId())
                .map(Product::getSku)
                .orElse("N/A");
        String locName = locationRepository.findById(q.getLocationId())
                .map(StockLocation::getCompleteName)
                .orElse("Unknown Location");
        String lotName = q.getLotId() != null ? lotRepository.findById(q.getLotId())
                .map(StockLot::getName)
                .orElse("N/A") : "N/A";
        return InventoryMapper.toDto(q, pName, pSku, locName, lotName);
    }
}
