package com.manacommunity.api.inventory.service;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.entity.*;
import com.manacommunity.api.inventory.exception.InsufficientStockException;
import com.manacommunity.api.inventory.exception.StockPickingException;
import com.manacommunity.api.inventory.mapper.InventoryMapper;
import com.manacommunity.api.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockTransferService {

    private final StockPickingRepository pickingRepository;
    private final StockMoveRepository moveRepository;
    private final StockMoveLineRepository moveLineRepository;
    private final StockPickingTypeRepository pickingTypeRepository;
    private final StockLocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final StockQuantService quantService;
    private final StockLotRepository lotRepository;

    @Transactional(readOnly = true)
    public List<PickingDto> getAllPickings() {
        return pickingRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PickingDto getPickingById(Long id) {
        StockPicking p = pickingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Picking not found with id: " + id));
        return mapToDto(p);
    }

    @Transactional
    public PickingDto createPicking(PickingCreateRequest request) {
        StockPickingType type = pickingTypeRepository.findById(request.getPickingTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Picking type not found: " + request.getPickingTypeId()));

        Long srcLocId = request.getLocationSrcId() != null ? request.getLocationSrcId() : type.getDefaultLocationSrcId();
        Long destLocId = request.getLocationDestId() != null ? request.getLocationDestId() : type.getDefaultLocationDestId();

        if (srcLocId == null || destLocId == null) {
            throw new StockPickingException("Source and Destination locations must be specified.");
        }

        // Generate clean unique transfer reference name
        String prefix = type.getCode().name().toUpperCase();
        long count = pickingRepository.count() + 1;
        String name = String.format("WH/%s/%05d", prefix, count);

        StockPicking picking = StockPicking.builder()
                .name(name)
                .origin(request.getOrigin())
                .state(PickingState.draft)
                .scheduledDate(request.getScheduledDate() != null ? request.getScheduledDate() : LocalDateTime.now())
                .locationId(srcLocId)
                .locationDestId(destLocId)
                .pickingTypeId(type.getId())
                .build();

        picking = pickingRepository.save(picking);

        List<StockMove> moves = new ArrayList<>();
        if (request.getLines() != null) {
            for (PickingCreateRequest.LineRequest line : request.getLines()) {
                StockMove move = StockMove.builder()
                        .productId(line.getProductId())
                        .productUomQty(line.getQuantity())
                        .quantity(0.0) // not executed yet
                        .state(MoveState.draft)
                        .pickingId(picking.getId())
                        .locationId(srcLocId)
                        .build();
                moves.add(moveRepository.save(move));
            }
        }

        PickingDto result = mapToDto(picking);
        result.setMoves(moves.stream()
                .map(m -> {
                    String pName = productRepository.findById(m.getProductId()).map(Product::getName).orElse("Unknown");
                    String lName = locationRepository.findById(m.getLocationId()).map(StockLocation::getCompleteName).orElse("Unknown");
                    return InventoryMapper.toDto(m, pName, lName);
                })
                .collect(Collectors.toList()));
        return result;
    }

    @Transactional
    public PickingDto confirmPicking(Long id) {
        StockPicking p = pickingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Picking not found: " + id));

        if (p.getState() != PickingState.draft) {
            throw new StockPickingException("Only draft pickings can be confirmed.");
        }

        p.setState(PickingState.ready);
        pickingRepository.save(p);

        List<StockMove> moves = moveRepository.findByPickingId(id);
        for (StockMove move : moves) {
            move.setState(MoveState.assigned);
            moveRepository.save(move);
        }

        return mapToDto(p);
    }

    @Transactional
    public PickingDto validatePicking(Long id, List<PickingCreateRequest.LineRequest> lineExecutions) {
        StockPicking p = pickingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Picking not found: " + id));

        if (p.getState() == PickingState.done || p.getState() == PickingState.cancel) {
            throw new StockPickingException("Picking cannot be validated in its current state: " + p.getState());
        }

        List<StockMove> moves = moveRepository.findByPickingId(id);
        if (moves.isEmpty()) {
            throw new StockPickingException("Picking contains no moves to validate.");
        }

        // Auto-confirm if still in draft
        if (p.getState() == PickingState.draft) {
            p.setState(PickingState.ready);
            for (StockMove m : moves) {
                m.setState(MoveState.assigned);
                moveRepository.save(m);
            }
        }

        // Loop over each line execution detail
        for (StockMove move : moves) {
            // Find execution request matching product
            PickingCreateRequest.LineRequest exec = null;
            if (lineExecutions != null) {
                exec = lineExecutions.stream()
                        .filter(l -> l.getProductId().equals(move.getProductId()))
                        .findFirst()
                        .orElse(null);
            }

            Double qtyDone = exec != null ? exec.getQuantity() : move.getProductUomQty();
            String serialNum = exec != null ? exec.getSerialNumber() : null;

            // Handle lot / serial allocation
            Long lotId = null;
            if (serialNum != null && !serialNum.trim().isEmpty()) {
                StockLot lot = lotRepository.findByProductIdAndName(move.getProductId(), serialNum)
                        .orElseGet(() -> lotRepository.save(StockLot.builder()
                                .name(serialNum)
                                .productId(move.getProductId())
                                .productQty(0.0)
                                .build()));
                lotId = lot.getId();
            }

            // Perform double entry stock adjustments
            // Decrement stock from Source Location (unless it is a vendor/incoming supplier)
            StockLocation srcLoc = locationRepository.findById(p.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException("Source location not found"));

            if (srcLoc.getUsage() != LocationUsage.vendor && srcLoc.getUsage() != LocationUsage.inventory) {
                quantService.adjustStock(move.getProductId(), p.getLocationId(), lotId, -qtyDone);
            }

            // Increment stock in Destination Location (unless it is a customer/outgoing consumer)
            StockLocation destLoc = locationRepository.findById(p.getLocationDestId())
                    .orElseThrow(() -> new IllegalArgumentException("Destination location not found"));

            if (destLoc.getUsage() != LocationUsage.customer && destLoc.getUsage() != LocationUsage.scrap) {
                quantService.adjustStock(move.getProductId(), p.getLocationDestId(), lotId, qtyDone);
            }

            // If it is scrapped, we log it under Virtual/Scrap and decrease standard stock
            if (destLoc.getUsage() == LocationUsage.scrap) {
                quantService.adjustStock(move.getProductId(), p.getLocationDestId(), lotId, qtyDone);
            }

            // Create move line log (history ledger)
            StockMoveLine moveLine = StockMoveLine.builder()
                    .productId(move.getProductId())
                    .lotId(lotId)
                    .quantity(qtyDone)
                    .locationId(p.getLocationId())
                    .locationDestId(p.getLocationDestId())
                    .moveId(move.getId())
                    .pickingId(p.getId())
                    .build();
            moveLineRepository.save(moveLine);

            // Complete move state
            move.setQuantity(qtyDone);
            move.setState(MoveState.done);
            moveRepository.save(move);
        }

        p.setState(PickingState.done);
        p.setUpdatedAt(LocalDateTime.now());
        pickingRepository.save(p);

        return mapToDto(p);
    }

    private PickingDto mapToDto(StockPicking p) {
        String srcLocName = locationRepository.findById(p.getLocationId())
                .map(StockLocation::getCompleteName)
                .orElse("Unknown Source");
        String destLocName = locationRepository.findById(p.getLocationDestId())
                .map(StockLocation::getCompleteName)
                .orElse("Unknown Destination");
        String typeName = pickingTypeRepository.findById(p.getPickingTypeId())
                .map(StockPickingType::getName)
                .orElse("Unknown Operation Type");

        PickingDto dto = InventoryMapper.toDto(p, srcLocName, destLocName, typeName);

        // Fetch child moves
        List<StockMove> moves = moveRepository.findByPickingId(p.getId());
        dto.setMoves(moves.stream()
                .map(m -> {
                    String pName = productRepository.findById(m.getProductId()).map(Product::getName).orElse("Unknown");
                    String lName = locationRepository.findById(m.getLocationId()).map(StockLocation::getCompleteName).orElse("Unknown");
                    return InventoryMapper.toDto(m, pName, lName);
                })
                .collect(Collectors.toList()));

        // Fetch child move lines
        List<StockMoveLine> lines = moveLineRepository.findByPickingId(p.getId());
        dto.setMoveLines(lines.stream()
                .map(ml -> {
                    String pName = productRepository.findById(ml.getProductId()).map(Product::getName).orElse("Unknown");
                    String lotName = ml.getLotId() != null ? lotRepository.findById(ml.getLotId()).map(StockLot::getName).orElse("N/A") : "N/A";
                    String sName = locationRepository.findById(ml.getLocationId()).map(StockLocation::getCompleteName).orElse("Unknown");
                    String dName = locationRepository.findById(ml.getLocationDestId()).map(StockLocation::getCompleteName).orElse("Unknown");
                    return InventoryMapper.toDto(ml, pName, lotName, sName, dName);
                })
                .collect(Collectors.toList()));

        return dto;
    }
}
