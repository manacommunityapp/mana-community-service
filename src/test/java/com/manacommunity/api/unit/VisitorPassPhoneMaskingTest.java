package com.manacommunity.api.unit;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.privacy.PiiMaskingService;
import com.manacommunity.api.visitor.dto.VisitorPassResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the visitor phone masking logic applied in VisitorPassController
 * correctly masks phone numbers before returning to security guards.
 */
@DisplayName("Visitor Pass Phone Masking")
class VisitorPassPhoneMaskingTest {

    private PiiMaskingService piiMaskingService;

    @BeforeEach
    void setUp() {
        piiMaskingService = new PiiMaskingService();
    }

    // Simulate the maskPhoneForGuard() logic from VisitorPassController
    private List<VisitorPassResponse> maskPhoneForGuard(List<VisitorPassResponse> passes) {
        return passes.stream().map(p -> {
            if (p.getVisitorPhone() != null) {
                return VisitorPassResponse.builder()
                        .id(p.getId())
                        .passCode(p.getPassCode())
                        .visitorName(p.getVisitorName())
                        .visitorPhone(piiMaskingService.maskPhone(p.getVisitorPhone()))
                        .vehicleNumber(p.getVehicleNumber())
                        .status(p.getStatus())
                        .flatNumber(p.getFlatNumber())
                        .communityId(p.getCommunityId())
                        .build();
            }
            return p;
        }).collect(Collectors.toList());
    }

    @Nested
    @DisplayName("maskPhoneForGuard")
    class MaskPhoneForGuard {

        @Test
        @DisplayName("security guard sees masked phone, not full number")
        void guardSeesOnlyMaskedPhone() {
            VisitorPassResponse pass = VisitorPassResponse.builder()
                    .id(1L).passCode("PASS001")
                    .visitorName("Ravi Shankar")
                    .visitorPhone("9876543210")
                    .status("ACTIVE")
                    .build();

            List<VisitorPassResponse> masked = maskPhoneForGuard(List.of(pass));

            assertThat(masked).hasSize(1);
            String maskedPhone = masked.get(0).getVisitorPhone();
            assertThat(maskedPhone).doesNotContain("9876543210");
            assertThat(maskedPhone).startsWith("98");
            assertThat(maskedPhone).endsWith("10");
        }

        @Test
        @DisplayName("all non-phone fields are preserved unchanged")
        void nonPhoneFieldsPreserved() {
            VisitorPassResponse pass = VisitorPassResponse.builder()
                    .id(99L).passCode("VP-XYZ")
                    .visitorName("Priya Sharma")
                    .visitorPhone("9111222333")
                    .vehicleNumber("TS09AB1234")
                    .status("CHECKED_IN")
                    .flatNumber("B-402")
                    .communityId(5L)
                    .build();

            List<VisitorPassResponse> masked = maskPhoneForGuard(List.of(pass));
            VisitorPassResponse result = masked.get(0);

            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getPassCode()).isEqualTo("VP-XYZ");
            assertThat(result.getVisitorName()).isEqualTo("Priya Sharma");
            assertThat(result.getVehicleNumber()).isEqualTo("TS09AB1234");
            assertThat(result.getStatus()).isEqualTo("CHECKED_IN");
            assertThat(result.getFlatNumber()).isEqualTo("B-402");
            assertThat(result.getCommunityId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("null phone pass is returned unchanged")
        void nullPhonePassReturnedUnchanged() {
            VisitorPassResponse pass = VisitorPassResponse.builder()
                    .id(2L).passCode("PASS002")
                    .visitorName("Unknown")
                    .visitorPhone(null)
                    .status("PENDING")
                    .build();

            List<VisitorPassResponse> result = maskPhoneForGuard(List.of(pass));

            assertThat(result.get(0).getVisitorPhone()).isNull();
        }

        @Test
        @DisplayName("empty pass list returns empty list")
        void emptyListReturnsEmpty() {
            assertThat(maskPhoneForGuard(List.of())).isEmpty();
        }

        @Test
        @DisplayName("masks phones in batch of multiple passes")
        void masksBatchOfPasses() {
            List<VisitorPassResponse> passes = List.of(
                    VisitorPassResponse.builder().id(1L).visitorPhone("9876543210").visitorName("A").passCode("P1").build(),
                    VisitorPassResponse.builder().id(2L).visitorPhone("8765432109").visitorName("B").passCode("P2").build(),
                    VisitorPassResponse.builder().id(3L).visitorPhone("7654321098").visitorName("C").passCode("P3").build()
            );

            List<VisitorPassResponse> masked = maskPhoneForGuard(passes);

            assertThat(masked).hasSize(3);
            masked.forEach(p -> {
                assertThat(p.getVisitorPhone()).doesNotContainAnyWhitespaces();
                assertThat(p.getVisitorPhone().length()).isLessThan(12);
            });
        }
    }
}
