package com.manacommunity.api.unit.service;

import com.manacommunity.api.commerce.core.model.CommerceChannel;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderItem;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.repository.CommerceOrderItemRepository;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.fresh.catalog.model.DeliveryMode;
import com.manacommunity.api.fresh.catalog.model.FreshProductCatalogProfile;
import com.manacommunity.api.fresh.catalog.model.FulfilmentOwnerType;
import com.manacommunity.api.fresh.catalog.repository.FreshProductCatalogProfileRepository;
import com.manacommunity.api.fresh.fulfilment.dto.*;
import com.manacommunity.api.fresh.fulfilment.model.*;
import com.manacommunity.api.fresh.fulfilment.repository.*;
import com.manacommunity.api.fresh.fulfilment.service.FreshFulfilmentGroupService;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.commerce.repository.VendorProductRepository;
import com.manacommunity.api.vendor.entity.VmsVendor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreshFulfilmentGroupServiceTest {

    @Mock
    private FreshFulfilmentGroupRepository groupRepository;
    @Mock
    private FreshFulfilmentItemRepository itemRepository;
    @Mock
    private CommerceOrderRepository orderRepository;
    @Mock
    private CommerceOrderItemRepository orderItemRepository;
    @Mock
    private VendorProductRepository vendorProductRepository;
    @Mock
    private FreshProductCatalogProfileRepository catalogProfileRepository;

    @InjectMocks
    private FreshFulfilmentGroupService fulfilmentGroupService;

    private CommerceOrder mockOrder;
    private CommerceOrderItem item1;
    private CommerceOrderItem item2;
    private VendorProduct vp1;
    private VendorProduct vp2;
    private VmsVendor vendor1;
    private VmsVendor vendor2;

    @BeforeEach
    void setUp() {
        AppUser buyer = AppUser.builder().id(101L).fullName("Sandeep Kumar").build();
        Community comm = Community.builder().id(1L).name("My Home Jewel").build();

        mockOrder = CommerceOrder.builder()
                .id(500L)
                .orderNumber("ORD-202611-001")
                .buyer(buyer)
                .community(comm)
                .channel(CommerceChannel.MARKETPLACE)
                .status(CommerceOrderStatus.CONFIRMED)
                .subtotalAmount(new BigDecimal("350.00"))
                .totalAmount(new BigDecimal("350.00"))
                .build();

        vendor1 = VmsVendor.builder().id(10L).businessName("Pure Desi Dairy").build();
        vendor2 = VmsVendor.builder().id(20L).businessName("Mana Cuts & Salads").build();

        vp1 = VendorProduct.builder().id(1001L).name("A2 Farm Fresh Milk").vendor(vendor1).build();
        vp2 = VendorProduct.builder().id(1002L).name("Cut Watermelon Bowl").vendor(vendor2).build();

        item1 = CommerceOrderItem.builder()
                .id(1L)
                .order(mockOrder)
                .productId("1001")
                .title("A2 Farm Fresh Milk")
                .unitPrice(new BigDecimal("50.00"))
                .quantity(2)
                .totalPrice(new BigDecimal("100.00"))
                .packSize("L")
                .build();

        item2 = CommerceOrderItem.builder()
                .id(2L)
                .order(mockOrder)
                .productId("1002")
                .title("Cut Watermelon Bowl")
                .unitPrice(new BigDecimal("250.00"))
                .quantity(1)
                .totalPrice(new BigDecimal("250.00"))
                .packSize("PACK")
                .build();
    }

    @Test
    @DisplayName("splitOrderIntoFulfilmentGroups creates distinct groups for different vendors")
    void testSplitOrderIntoFulfilmentGroups() {
        when(orderRepository.findById(500L)).thenReturn(Optional.of(mockOrder));
        when(groupRepository.findByOrderId(500L)).thenReturn(Collections.emptyList());
        when(orderItemRepository.findByOrderId(500L)).thenReturn(Arrays.asList(item1, item2));
        when(vendorProductRepository.findById(1001L)).thenReturn(Optional.of(vp1));
        when(vendorProductRepository.findById(1002L)).thenReturn(Optional.of(vp2));

        FreshProductCatalogProfile profile1 = FreshProductCatalogProfile.builder()
                .fulfilmentOwnerType(FulfilmentOwnerType.VENDOR)
                .deliveryMode(DeliveryMode.VENDOR_DELIVERY)
                .build();

        FreshProductCatalogProfile profile2 = FreshProductCatalogProfile.builder()
                .fulfilmentOwnerType(FulfilmentOwnerType.MANA)
                .deliveryMode(DeliveryMode.MANA_DELIVERY)
                .build();

        when(catalogProfileRepository.findByVendorProductId(1001L)).thenReturn(Optional.of(profile1));
        when(catalogProfileRepository.findByVendorProductId(1002L)).thenReturn(Optional.of(profile2));

        when(groupRepository.save(any(FreshFulfilmentGroup.class))).thenAnswer(inv -> {
            FreshFulfilmentGroup g = inv.getArgument(0);
            g.setId(new Random().nextLong(100, 999));
            return g;
        });

        List<FulfilmentGroupDto> groups = fulfilmentGroupService.splitOrderIntoFulfilmentGroups(500L);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(0).getOrderNumber()).isEqualTo("ORD-202611-001");
        assertThat(groups.get(0).getHandoverOtp()).isNotNull().hasSize(6);
        assertThat(groups.get(0).getQrCodeToken()).startsWith("FFG-");

        verify(groupRepository, times(2)).save(any(FreshFulfilmentGroup.class));
        verify(itemRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("verifyHandover succeeds with valid OTP and marks group DELIVERED")
    void testVerifyHandoverSuccess() {
        FreshFulfilmentGroup group = FreshFulfilmentGroup.builder()
                .id(1L)
                .groupReference("GRP-ORD-202611-001-1")
                .order(mockOrder)
                .status(FulfilmentGroupStatus.DISPATCHED)
                .handoverOtp("482910")
                .qrCodeToken("FFG-TEST-QR")
                .items(new ArrayList<>())
                .build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(FreshFulfilmentGroup.class))).thenReturn(group);

        VerifyHandoverRequest req = VerifyHandoverRequest.builder().otp("482910").build();
        FulfilmentGroupDto result = fulfilmentGroupService.verifyHandover(1L, req);

        assertThat(result.getStatus()).isEqualTo(FulfilmentGroupStatus.DELIVERED);
        assertThat(result.getDeliveredAt()).isNotNull();
    }

    @Test
    @DisplayName("verifyHandover rejects incorrect OTP")
    void testVerifyHandoverRejectsInvalidOtp() {
        FreshFulfilmentGroup group = FreshFulfilmentGroup.builder()
                .id(1L)
                .groupReference("GRP-ORD-202611-001-1")
                .order(mockOrder)
                .status(FulfilmentGroupStatus.DISPATCHED)
                .handoverOtp("482910")
                .build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        VerifyHandoverRequest req = VerifyHandoverRequest.builder().otp("000000").build();
        assertThatThrownBy(() -> fulfilmentGroupService.verifyHandover(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid OTP or QR token");
    }
}
