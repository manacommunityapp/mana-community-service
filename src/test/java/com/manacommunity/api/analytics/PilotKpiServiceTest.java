package com.manacommunity.api.analytics;

import com.manacommunity.api.analytics.dto.PilotKpisResponse;
import com.manacommunity.api.analytics.repository.ProductAnalyticsEventRepository;
import com.manacommunity.api.analytics.service.PilotKpiService;
import com.manacommunity.api.commerce.core.model.CommerceSubscription;
import com.manacommunity.api.commerce.core.repository.CommerceSubscriptionRepository;
import com.manacommunity.api.helpdesk.entity.Ticket;
import com.manacommunity.api.helpdesk.repository.TicketRepository;
import com.manacommunity.api.model.Vendor;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.repository.PaymentOrderRepository;
import com.manacommunity.api.repository.VendorRepository;
import com.manacommunity.api.user.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PilotKpiService")
class PilotKpiServiceTest {

    @Mock private ProductAnalyticsEventRepository eventRepo;
    @Mock private PaymentOrderRepository paymentOrderRepo;
    @Mock private TicketRepository ticketRepo;
    @Mock private VendorRepository vendorRepo;
    @Mock private CommerceSubscriptionRepository subscriptionRepo;
    @Mock private AppUserRepository appUserRepo;

    @InjectMocks
    private PilotKpiService pilotKpiService;

    @Test
    @DisplayName("getPilotKpis aggregates all six pilot measurement domains")
    void getPilotKpisAggregatesCorrectly() {
        Long communityId = 1L;
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 20);

        when(eventRepo.countDistinctActiveUsers(eq(communityId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(42L);
        when(appUserRepo.findByCommunityId(communityId)).thenReturn(List.of());
        when(eventRepo.countByEventName(eq(communityId), eq("signup_completed"), any(), any()))
                .thenReturn(15L);
        when(eventRepo.countDistinctUsersByEventName(eq(communityId), eq("first_key_action"), any(), any()))
                .thenReturn(10L);

        when(paymentOrderRepo.countByCommunityIdAndCreatedAtBetween(eq(communityId), any(), any()))
                .thenReturn(25L);
        when(paymentOrderRepo.countByCommunityIdAndStatusAndCreatedAtBetween(eq(communityId), eq(PaymentOrderStatus.SUCCESS), any(), any()))
                .thenReturn(23L);
        when(paymentOrderRepo.countByCommunityIdAndStatusAndCreatedAtBetween(eq(communityId), eq(PaymentOrderStatus.FAILED), any(), any()))
                .thenReturn(2L);
        when(paymentOrderRepo.sumAmountByCommunityIdAndStatusAndCreatedAtBetween(eq(communityId), eq(PaymentOrderStatus.SUCCESS), any(), any()))
                .thenReturn(BigDecimal.valueOf(50000));

        when(vendorRepo.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(
                Vendor.builder().id(101L).name("Acme Services").rating(5).build()
        ));

        Ticket ticket = Ticket.builder()
                .id(1L)
                .status(Ticket.TicketStatus.RESOLVED)
                .resolvedAt(LocalDateTime.now())
                .build();
        ticket.setCreatedAt(LocalDateTime.now().minusHours(3));
        when(ticketRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)).thenReturn(List.of(ticket));

        when(subscriptionRepo.findByCommunityIdOrderByCreatedAtDesc(communityId)).thenReturn(List.of(
                CommerceSubscription.builder().status("ACTIVE").unitPrice(BigDecimal.valueOf(500)).quantity(2).build()
        ));

        List<Object[]> refRows = new java.util.ArrayList<>();
        refRows.add(new Object[]{"AMENITY_BOOKING", 10L, BigDecimal.valueOf(15000)});
        when(paymentOrderRepo.analyticsGroupByReferenceType(eq(communityId), any(), any())).thenReturn(refRows);

        PilotKpisResponse response = pilotKpiService.getPilotKpis(communityId, from, to);

        assertThat(response).isNotNull();
        assertThat(response.getActiveResidents()).isEqualTo(42L);
        assertThat(response.getSignups()).isEqualTo(15L);
        assertThat(response.getActivatedResidents()).isEqualTo(10L);

        // Funnel
        assertThat(response.getFunnel()).isNotEmpty();

        // Transactions
        assertThat(response.getTransactions().getPaymentsStarted()).isEqualTo(25L);
        assertThat(response.getTransactions().getPaymentsSucceeded()).isEqualTo(23L);
        assertThat(response.getTransactions().getPaymentsFailed()).isEqualTo(2L);

        // Vendors
        assertThat(response.getVendors()).hasSize(1);
        assertThat(response.getVendors().get(0).getName()).isEqualTo("Acme Services");

        // Support
        assertThat(response.getSupport().getTickets()).isEqualTo(1L);
        assertThat(response.getSupport().getAvgResolutionHours()).isNotNull();

        // Revenue
        assertThat(response.getRevenue().getGmv()).isEqualByComparingTo("50000");
        assertThat(response.getRevenue().getCommission()).isEqualByComparingTo("2500.00");
        assertThat(response.getRevenue().getSubscribers()).isEqualTo(1L);
        assertThat(response.getRevenue().getSubscriptionMrr()).isEqualByComparingTo("1000");

        // Modules
        assertThat(response.getModules()).isNotEmpty();
        PilotKpisResponse.ModuleKpiDto amenityMod = response.getModules().stream()
                .filter(m -> "AMENITY_BOOKING".equals(m.getModule()))
                .findFirst().orElse(null);
        assertThat(amenityMod).isNotNull();
        assertThat(amenityMod.getRevenue()).isEqualByComparingTo("15000");
        assertThat(amenityMod.getCost()).isGreaterThan(BigDecimal.ZERO);
        assertThat(amenityMod.getOrderCount()).isEqualTo(10L);

        // Calibrated cost model defaults
        assertThat(response.getCostCalibration()).isNotNull();
        assertThat(response.getCostCalibration().getGatewayMdrRate()).isEqualTo(0.02);
        assertThat(response.getCostCalibration().getPerTxnFixedCost()).isEqualTo(2.0);

        // Custom Finance/Ops calibration verification
        PilotKpisResponse customResponse = pilotKpiService.getPilotKpis(
                communityId, from, to, 0.015, 1.0, 100.0);
        assertThat(customResponse.getCostCalibration().getGatewayMdrRate()).isEqualTo(0.015);
        assertThat(customResponse.getCostCalibration().getPerTxnFixedCost()).isEqualTo(1.0);
        assertThat(customResponse.getCostCalibration().getModuleHostingAllocation()).isEqualTo(100.0);
    }
}
