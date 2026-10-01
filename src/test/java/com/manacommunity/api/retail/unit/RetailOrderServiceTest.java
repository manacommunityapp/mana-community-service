package com.manacommunity.api.retail.unit;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.RetailOrderDto;
import com.manacommunity.api.retail.entity.RetailOrder;
import com.manacommunity.api.retail.entity.RetailProduct;
import com.manacommunity.api.retail.repository.RetailOrderRepository;
import com.manacommunity.api.retail.repository.RetailProductRepository;
import com.manacommunity.api.retail.service.CustomerService;
import com.manacommunity.api.retail.service.RetailOrderService;
import com.manacommunity.api.retail.service.SupplierService;
import com.manacommunity.api.security.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Retail Order Service Unit Tests")
class RetailOrderServiceTest {

    @Mock
    private RetailOrderRepository orderRepository;

    @Mock
    private RetailProductRepository productRepository;

    @Mock
    private SupplierService supplierService;

    @Mock
    private CustomerService customerService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RetailOrderService orderService;

    private Community testCommunity;
    private RetailProduct testProduct;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(1L).name("Mana Residency").build();
        testProduct = RetailProduct.builder()
                .id(100L)
                .name("Basmati Rice 5kg")
                .unitPrice(new BigDecimal("450.00"))
                .unitsOrdered(50)
                .unitsSold(10)
                .build();
    }

    @Test
    @DisplayName("Should create sales order and assign code")
    void shouldCreateSalesOrder() {
        RetailOrderDto dto = RetailOrderDto.builder()
                .type("SALES")
                .partyId(5L)
                .orderDate("2026-10-01")
                .status("OPEN")
                .items(List.of(
                        RetailOrderDto.LineDto.builder()
                                .productId(100L)
                                .qty(2)
                                .unitPrice(new BigDecimal("450.00"))
                                .build()
                ))
                .build();

        when(orderRepository.countByCommunityIdAndOrderType(1L, RetailOrder.OrderType.SALES)).thenReturn(4L);
        when(orderRepository.save(any(RetailOrder.class))).thenAnswer(inv -> {
            RetailOrder o = inv.getArgument(0);
            o.setId(201L);
            return o;
        });
        when(customerService.getCustomerName(5L)).thenReturn("John Doe");
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));

        RetailOrderDto created = orderService.createOrder(dto, testCommunity);

        assertNotNull(created);
        assertEquals("SO5", created.getCode());
        assertEquals("John Doe", created.getPartyName());
        assertEquals(new BigDecimal("900.00"), created.getTotal());
    }

    @Test
    @DisplayName("Should update product counters when order fulfilled")
    void shouldUpdateProductCountersOnFulfillment() {
        RetailOrder order = RetailOrder.builder()
                .id(301L)
                .code("SO-10")
                .orderType(RetailOrder.OrderType.SALES)
                .partyId(5L)
                .orderDate(LocalDate.now())
                .status(RetailOrder.OrderStatus.OPEN)
                .community(testCommunity)
                .lines(new ArrayList<>())
                .build();

        when(orderRepository.findById(301L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(RetailOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findById(100L)).thenReturn(Optional.of(testProduct));
        when(customerService.getCustomerName(5L)).thenReturn("John Doe");

        RetailOrderDto updateDto = RetailOrderDto.builder()
                .type("SALES")
                .partyId(5L)
                .orderDate("2026-10-01")
                .status("FULFILLED")
                .items(List.of(
                        RetailOrderDto.LineDto.builder()
                                .productId(100L)
                                .qty(5)
                                .unitPrice(new BigDecimal("450.00"))
                                .build()
                ))
                .build();

        orderService.updateOrder(301L, updateDto, 1L);

        // units sold should have incremented from 10 to 15
        assertEquals(15, testProduct.getUnitsSold());
        verify(productRepository).save(testProduct);
    }
}
