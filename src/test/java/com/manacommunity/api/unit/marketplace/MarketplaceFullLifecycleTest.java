package com.manacommunity.api.unit.marketplace;

import com.manacommunity.api.cfbos.wallet.engine.WalletEngine;
import com.manacommunity.api.cfbos.wallet.enums.WalletTransactionType;
import com.manacommunity.api.marketplace.dto.MarketSellerDtos.*;
import com.manacommunity.api.marketplace.entity.*;
import com.manacommunity.api.marketplace.repository.*;
import com.manacommunity.api.marketplace.service.*;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceFullLifecycleTest {

    @Mock
    private MarketSellerRepository sellerRepository;

    @Mock
    private MarketInventoryReservationRepository reservationRepository;

    @Mock
    private MarketListingRepository listingRepository;

    @Mock
    private MarketplaceSettlementRepository settlementRepository;

    @Mock
    private WalletEngine walletEngine;

    @InjectMocks
    private MarketSellerService sellerService;

    @InjectMocks
    private MarketInventoryEngine inventoryEngine;

    @InjectMocks
    private MarketplaceSettlementService settlementService;

    private AppUser sellerUser;
    private AppUser buyerUser;
    private Community testCommunity;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder().id(1L).name("Mana Residency").build();

        sellerUser = AppUser.builder()
                .id(10L)
                .fullName("Mary Baker")
                .phone("+919876543210")
                .email("mary@bakes.com")
                .community(testCommunity)
                .build();

        buyerUser = AppUser.builder()
                .id(20L)
                .fullName("John Resident")
                .email("john@example.com")
                .community(testCommunity)
                .build();
    }

    @Test
    @DisplayName("Stage 1 & 2: Seller registration and KYC approval lifecycle")
    void sellerRegistrationAndKycApproval() {
        SellerRegisterRequest regReq = SellerRegisterRequest.builder()
                .businessName("Mary's Artisan Bakes")
                .sellerType(MarketSellerProfile.SellerType.HOMEPRENEUR)
                .storeDescription("Fresh sourdough and custom cakes")
                .flatNumber("Tower 2 - 402")
                .build();

        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(sellerRepository.save(any(MarketSellerProfile.class))).thenAnswer(i -> {
            MarketSellerProfile p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        SellerProfileResponse regResponse = sellerService.registerSeller(sellerUser, regReq);
        assertThat(regResponse.getId()).isEqualTo(1L);
        assertThat(regResponse.getBusinessName()).isEqualTo("Mary's Artisan Bakes");
        assertThat(regResponse.getKycStatus()).isEqualTo(MarketSellerProfile.KycStatus.UNVERIFIED);

        // Submit KYC
        MarketSellerProfile savedProfile = MarketSellerProfile.builder()
                .id(1L)
                .user(sellerUser)
                .businessName("Mary's Artisan Bakes")
                .kycStatus(MarketSellerProfile.KycStatus.UNVERIFIED)
                .build();

        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(savedProfile));

        SellerKycSubmitRequest kycReq = SellerKycSubmitRequest.builder()
                .fssaiLicenseNumber("FSSAI-123456789")
                .panNumber("ABCDE1234F")
                .bankAccountNumber("987654321012")
                .bankIfscCode("HDFC0001234")
                .bankAccountHolderName("Mary Baker")
                .build();

        SellerProfileResponse kycResponse = sellerService.submitKyc(sellerUser, kycReq);
        assertThat(kycResponse.getKycStatus()).isEqualTo(MarketSellerProfile.KycStatus.SUBMITTED);

        // Admin approves KYC
        when(sellerRepository.findById(1L)).thenReturn(Optional.of(savedProfile));
        KycDecisionRequest decision = KycDecisionRequest.builder()
                .decision(MarketSellerProfile.KycStatus.VERIFIED)
                .build();

        SellerProfileResponse decisionResponse = sellerService.processKycDecision(1L, decision);
        assertThat(decisionResponse.getKycStatus()).isEqualTo(MarketSellerProfile.KycStatus.VERIFIED);
    }

    @Test
    @DisplayName("Stage 3 & 4: Inventory stock reservation and hold commit")
    void inventoryStockReservationAndCommit() {
        MarketListing listing = MarketListing.builder()
                .id(100L)
                .title("Chocolate Truffle Cake")
                .price(BigDecimal.valueOf(800))
                .availableQuantity(5)
                .status(MarketListing.ListingStatus.ACTIVE)
                .build();

        when(listingRepository.findById(100L)).thenReturn(Optional.of(listing));
        when(reservationRepository.countActiveReservedQuantity(eq(100L), any(LocalDateTime.class))).thenReturn(1);
        when(reservationRepository.save(any(MarketInventoryReservation.class))).thenAnswer(i -> i.getArgument(0));

        MarketInventoryReservation res = inventoryEngine.reserveStock(100L, 20L, 2, "ORD-TEST-001");
        assertThat(res.getStatus()).isEqualTo(MarketInventoryReservation.ReservationStatus.RESERVED);
        assertThat(res.getQuantity()).isEqualTo(2);

        // Commit reservation
        when(reservationRepository.findByOrderNumber("ORD-TEST-001")).thenReturn(java.util.List.of(res));
        inventoryEngine.commitReservation("ORD-TEST-001");
        assertThat(res.getStatus()).isEqualTo(MarketInventoryReservation.ReservationStatus.COMMITTED);
        assertThat(listing.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Stage 10 & 11: Settlement calculation and payout release to seller's wallet")
    void settlementCreationAndRelease() {
        when(settlementRepository.save(any(MarketplaceSettlementRecord.class))).thenAnswer(i -> {
            MarketplaceSettlementRecord s = i.getArgument(0);
            s.setId(50L);
            return s;
        });

        MarketplaceSettlementRecord settlement = settlementService.createSettlementEntry(
                1001L, "ORD-TEST-999", 10L, BigDecimal.valueOf(1000.00)
        );

        assertThat(settlement.getGrossAmount()).isEqualTo(BigDecimal.valueOf(1000.00));
        assertThat(settlement.getPlatformFee()).isEqualTo(BigDecimal.valueOf(20.00)); // 2%
        assertThat(settlement.getNetPayoutAmount()).isEqualTo(BigDecimal.valueOf(980.00));
        assertThat(settlement.getStatus()).isEqualTo(MarketplaceSettlementRecord.SettlementStatus.PENDING_CLEARANCE);

        // Release settlement upon delivery
        when(settlementRepository.findByOrderNumber("ORD-TEST-999")).thenReturn(Optional.of(settlement));
        MarketSellerProfile sellerProfile = MarketSellerProfile.builder()
                .id(1L)
                .user(sellerUser)
                .totalOrdersCompleted(4)
                .build();
        when(sellerRepository.findByUserId(10L)).thenReturn(Optional.of(sellerProfile));

        MarketplaceSettlementRecord released = settlementService.releaseSettlementToSeller("ORD-TEST-999");
        assertThat(released.getStatus()).isEqualTo(MarketplaceSettlementRecord.SettlementStatus.SETTLED);
        assertThat(released.getPayoutReference()).contains("WALLET-CR-");

        verify(walletEngine).creditWallet(
                eq(10L),
                eq(BigDecimal.valueOf(980.00)),
                eq(WalletTransactionType.TOPUP),
                eq("MARKETPLACE_SETTLEMENT"),
                eq(1001L),
                contains("ORD-TEST-999")
        );
        assertThat(sellerProfile.getTotalOrdersCompleted()).isEqualTo(5);
    }
}
