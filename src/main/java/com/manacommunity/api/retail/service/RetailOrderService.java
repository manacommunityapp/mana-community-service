package com.manacommunity.api.retail.service;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.RetailOrderDto;
import com.manacommunity.api.retail.entity.RetailOrder;
import com.manacommunity.api.retail.entity.RetailOrderLine;
import com.manacommunity.api.retail.repository.RetailOrderRepository;
import com.manacommunity.api.retail.repository.RetailProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class RetailOrderService {

    private final RetailOrderRepository orderRepository;
    private final RetailProductRepository productRepository;
    private final SupplierService supplierService;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public List<RetailOrderDto> getOrders(Long communityId, String type) {
        List<RetailOrder> orders;
        if (type != null && !type.isBlank()) {
            var orderType = RetailOrder.OrderType.valueOf(type.toUpperCase());
            orders = orderRepository.findByCommunityIdAndOrderTypeOrderByOrderDateDesc(communityId, orderType);
        } else {
            orders = orderRepository.findByCommunityIdOrderByOrderDateDesc(communityId);
        }
        return orders.stream().map(this::toDto).toList();
    }

    @Transactional
    public RetailOrderDto createOrder(RetailOrderDto dto, Community community) {
        var orderType = RetailOrder.OrderType.valueOf(dto.getType().toUpperCase());

        long count = orderRepository.countByCommunityIdAndOrderType(community.getId(), orderType);
        String prefix = orderType == RetailOrder.OrderType.PURCHASE ? "PO" : "SO";
        String code = prefix + String.valueOf(count + 1);

        RetailOrder order = RetailOrder.builder()
                .code(code)
                .orderType(orderType)
                .partyId(dto.getPartyId())
                .orderDate(LocalDate.parse(dto.getOrderDate()))
                .status(RetailOrder.OrderStatus.valueOf(dto.getStatus().toUpperCase()))
                .community(community)
                .build();

        if (dto.getItems() != null) {
            for (var lineDto : dto.getItems()) {
                RetailOrderLine line = RetailOrderLine.builder()
                        .order(order)
                        .productId(lineDto.getProductId())
                        .qty(lineDto.getQty())
                        .unitPrice(lineDto.getUnitPrice())
                        .build();
                order.getLines().add(line);
            }
        }

        order = orderRepository.save(order);
        updateProductCounters(order);
        return toDto(order);
    }

    @Transactional
    public RetailOrderDto updateOrder(Long id, RetailOrderDto dto) {
        RetailOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));

        var oldStatus = order.getStatus();
        order.setPartyId(dto.getPartyId());
        order.setOrderDate(LocalDate.parse(dto.getOrderDate()));
        order.setStatus(RetailOrder.OrderStatus.valueOf(dto.getStatus().toUpperCase()));

        order.getLines().clear();
        if (dto.getItems() != null) {
            for (var lineDto : dto.getItems()) {
                RetailOrderLine line = RetailOrderLine.builder()
                        .order(order)
                        .productId(lineDto.getProductId())
                        .qty(lineDto.getQty())
                        .unitPrice(lineDto.getUnitPrice())
                        .build();
                order.getLines().add(line);
            }
        }

        order = orderRepository.save(order);

        if (oldStatus != RetailOrder.OrderStatus.FULFILLED
                && order.getStatus() == RetailOrder.OrderStatus.FULFILLED) {
            updateProductCounters(order);
        }

        return toDto(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    private void updateProductCounters(RetailOrder order) {
        if (order.getStatus() != RetailOrder.OrderStatus.FULFILLED) return;

        for (var line : order.getLines()) {
            productRepository.findById(line.getProductId()).ifPresent(product -> {
                if (order.getOrderType() == RetailOrder.OrderType.PURCHASE) {
                    product.setUnitsOrdered(product.getUnitsOrdered() + line.getQty());
                } else {
                    product.setUnitsSold(product.getUnitsSold() + line.getQty());
                }
                productRepository.save(product);
            });
        }
    }

    private RetailOrderDto toDto(RetailOrder order) {
        String partyName;
        if (order.getOrderType() == RetailOrder.OrderType.PURCHASE) {
            partyName = supplierService.getSupplierName(order.getPartyId());
        } else {
            partyName = customerService.getCustomerName(order.getPartyId());
        }

        BigDecimal total = BigDecimal.ZERO;
        var lineDtos = order.getLines().stream().map(line -> {
            String productName = productRepository.findById(line.getProductId())
                    .map(p -> p.getName()).orElse("Unknown");
            return RetailOrderDto.LineDto.builder()
                    .productId(line.getProductId())
                    .productName(productName)
                    .qty(line.getQty())
                    .unitPrice(line.getUnitPrice())
                    .build();
        }).toList();

        for (var l : lineDtos) {
            total = total.add(l.getUnitPrice().multiply(BigDecimal.valueOf(l.getQty())));
        }

        return RetailOrderDto.builder()
                .id(order.getId())
                .code(order.getCode())
                .type(order.getOrderType().name())
                .partyId(order.getPartyId())
                .partyName(partyName)
                .orderDate(order.getOrderDate().toString())
                .status(order.getStatus().name())
                .items(lineDtos)
                .total(total)
                .build();
    }
}
