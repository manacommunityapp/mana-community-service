package com.manacommunity.api.retail.unit;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.retail.dto.RetailProductDto;
import com.manacommunity.api.retail.entity.RetailProduct;
import com.manacommunity.api.retail.repository.RetailProductRepository;
import com.manacommunity.api.retail.service.RetailProductService;
import com.manacommunity.api.security.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Retail Product Service Unit Tests")
class RetailProductServiceTest {

    @Mock
    private RetailProductRepository repository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private RetailProductService productService;

    private Community testCommunity;
    private RetailProduct testProduct;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(1L).name("Mana Residency").build();
        testProduct = RetailProduct.builder()
                .id(10L)
                .name("Mineral Water 5L")
                .category("Beverages")
                .unitPrice(new BigDecimal("65.00"))
                .reorderLevel(15)
                .unitsOrdered(100)
                .unitsSold(20)
                .community(testCommunity)
                .build();
    }

    @Test
    @DisplayName("Should get all products for a community")
    void shouldGetAllProducts() {
        when(repository.findByCommunityIdOrderByNameAsc(1L)).thenReturn(List.of(testProduct));

        List<RetailProductDto> products = productService.getAllProducts(1L);
        assertEquals(1, products.size());
        assertEquals("Mineral Water 5L", products.get(0).getName());
    }

    @Test
    @DisplayName("Should create product successfully")
    void shouldCreateProduct() {
        RetailProductDto dto = RetailProductDto.builder()
                .name("Mineral Water 5L")
                .category("Beverages")
                .unitPrice(new BigDecimal("65.00"))
                .reorderLevel(15)
                .build();

        when(repository.save(any(RetailProduct.class))).thenReturn(testProduct);

        RetailProductDto created = productService.createProduct(dto, testCommunity);
        assertNotNull(created);
        assertEquals(10L, created.getId());
        verify(auditService).record(any(), any(), eq("RetailProduct"), eq("10"));
    }

    @Test
    @DisplayName("Should update product successfully")
    void shouldUpdateProduct() {
        RetailProductDto dto = RetailProductDto.builder()
                .name("Mineral Water 5L (Updated)")
                .unitPrice(new BigDecimal("70.00"))
                .reorderLevel(20)
                .build();

        when(repository.findById(10L)).thenReturn(Optional.of(testProduct));
        when(repository.save(any(RetailProduct.class))).thenReturn(testProduct);

        RetailProductDto updated = productService.updateProduct(10L, dto, 1L);
        assertNotNull(updated);
        assertEquals("Mineral Water 5L (Updated)", testProduct.getName());
    }
}
