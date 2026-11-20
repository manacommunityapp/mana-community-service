package com.manacommunity.api.food.service;

import com.manacommunity.api.food.entity.DeliveryBatch;
import com.manacommunity.api.food.entity.DeliveryBatchOrder;
import com.manacommunity.api.food.repository.DeliveryBatchOrderRepository;
import com.manacommunity.api.food.repository.DeliveryBatchRepository;
import com.manacommunity.api.food.repository.DeliverySlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryBatchService {

    private final DeliveryBatchRepository batchRepo;
    private final DeliveryBatchOrderRepository batchOrderRepo;
    private final DeliverySlotRepository slotRepo;

    @Transactional
    public List<DeliveryBatch> createBatches(Long slotId, List<DeliveryBatchOrder> orders) {
        var slot = slotRepo.findById(slotId).orElseThrow();
        // Group orders by tower/block derived from flatNumber prefix (e.g. "A-101" → "A")
        Map<String, List<DeliveryBatchOrder>> byTower = orders.stream()
            .collect(Collectors.groupingBy(o -> {
                String flat = o.getFlatNumber();
                return flat.contains("-") ? flat.substring(0, flat.indexOf('-')) : "DEFAULT";
            }));
        int seq = 1;
        for (Map.Entry<String, List<DeliveryBatchOrder>> entry : byTower.entrySet()) {
            DeliveryBatch batch = DeliveryBatch.builder()
                .communityId(slot.getCommunityId()).vendorId(slot.getVendorId())
                .slotId(slotId).towerBlock(entry.getKey())
                .batchSequence(seq++).orderCount(entry.getValue().size())
                .createdAt(Instant.now()).build();
            DeliveryBatch saved = batchRepo.save(batch);
            for (DeliveryBatchOrder order : entry.getValue()) {
                order.setBatchId(saved.getId());
                batchOrderRepo.save(order);
            }
        }
        return batchRepo.findBySlotId(slotId);
    }

    @Transactional
    public void assignDeliveryPerson(Long batchId, Long userId) {
        batchRepo.findById(batchId).ifPresent(b -> { b.setAssignedTo(userId); batchRepo.save(b); });
    }

    @Transactional
    public void markDelivered(Long batchOrderId, String notes) {
        batchOrderRepo.findById(batchOrderId).ifPresent(o -> {
            o.setDeliveredAt(Instant.now());
            o.setDeliveryNotes(notes);
            batchOrderRepo.save(o);
        });
    }

    public List<DeliveryBatch> getBatchesForSlot(Long slotId) {
        return batchRepo.findBySlotId(slotId);
    }

    public List<DeliveryBatchOrder> getOrdersForBatch(Long batchId) {
        return batchOrderRepo.findByBatchId(batchId);
    }
}
