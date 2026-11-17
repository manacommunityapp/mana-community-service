package com.manacommunity.api.payments.config.service;

import com.manacommunity.api.model.CommunitySettings;
import com.manacommunity.api.payments.config.dto.CheckoutOptionsResponse;
import com.manacommunity.api.payments.config.dto.UpdateCommunityPaymentSettingsRequest;
import com.manacommunity.api.payments.config.dto.UpdatePlatformPaymentConfigRequest;
import com.manacommunity.api.payments.config.entity.PlatformPaymentConfig;
import com.manacommunity.api.payments.config.repository.PlatformPaymentConfigRepository;
import com.manacommunity.api.payments.exception.PaymentGatewayNotAllowedException;
import com.manacommunity.api.repository.CommunitySettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentConfigService {

    private final PlatformPaymentConfigRepository platformRepo;
    private final CommunitySettingsRepository     communitySettingsRepo;

    // ─── Checkout options ─────────────────────────────────────────────────────────

    /**
     * Returns the payment options a member sees at checkout for a given community.
     * Result is the intersection of platform-enabled gateways and community's allowed list.
     */
    @Transactional(readOnly = true)
    public CheckoutOptionsResponse getCheckoutOptions(Long communityId) {
        PlatformPaymentConfig platform = platformRepo.findFirstByOrderByIdAsc()
            .orElseThrow(() -> new IllegalStateException("Platform payment config not initialised"));
        CommunitySettings community = communitySettingsRepo.findByCommunityId(communityId)
            .orElseThrow(() -> new IllegalArgumentException("No community settings for communityId=" + communityId));

        List<String> platformEnabled = getEnabledGatewayNames(platform);
        String[] communityAllowedArr = community.getAllowedPaymentGateways();
        List<String> communityAllowed = communityAllowedArr != null
            ? Arrays.asList(communityAllowedArr) : List.of("CASHFREE");

        List<String> intersection = communityAllowed.stream()
            .filter(platformEnabled::contains)
            .collect(Collectors.toList());

        // Fall back to platform default if community has no overlap
        if (intersection.isEmpty() && !platformEnabled.isEmpty()) {
            intersection = new ArrayList<>(List.of(platform.getDefaultGateway()));
        }

        // Effective default: community preference > platform default
        String effectiveDefault = community.getDefaultGateway() != null
            ? community.getDefaultGateway()
            : platform.getDefaultGateway();
        if (!intersection.contains(effectiveDefault) && !intersection.isEmpty()) {
            effectiveDefault = intersection.get(0);
        }

        List<String> manualModes = List.of();
        if (community.isManualPaymentEnabled()) {
            String[] modes = community.getManualPaymentModes();
            manualModes = modes != null ? Arrays.asList(modes) : List.of("CASH", "UPI");
        }

        return CheckoutOptionsResponse.builder()
            .gatewayOptions(intersection)
            .defaultGateway(effectiveDefault)
            .manualPaymentEnabled(community.isManualPaymentEnabled())
            .manualPaymentModes(manualModes)
            .paymentInstructions(community.getPaymentInstructions())
            .convenienceFeePercent(platform.getConvenienceFeePercent())
            .maxTransactionAmount(platform.getMaxTransactionAmount())
            .build();
    }

    // ─── Platform config (super admin) ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public PlatformPaymentConfig getPlatformConfig() {
        return platformRepo.findFirstByOrderByIdAsc()
            .orElseThrow(() -> new IllegalStateException("Platform payment config not initialised"));
    }

    @Transactional
    public PlatformPaymentConfig updatePlatformConfig(UpdatePlatformPaymentConfigRequest req, Long adminId) {
        PlatformPaymentConfig config = getPlatformConfig();
        if (req.getCashfreeEnabled() != null)   config.setCashfreeEnabled(req.getCashfreeEnabled());
        if (req.getRazorpayEnabled() != null)   config.setRazorpayEnabled(req.getRazorpayEnabled());
        if (req.getManaPayEnabled() != null)    config.setManaPayEnabled(req.getManaPayEnabled());
        if (req.getBbpsEnabled() != null)       config.setBbpsEnabled(req.getBbpsEnabled());
        if (req.getDefaultGateway() != null)    config.setDefaultGateway(req.getDefaultGateway());
        if (req.getConvenienceFeePercent() != null) config.setConvenienceFeePercent(req.getConvenienceFeePercent());
        if (req.getMaxTransactionAmount() != null)  config.setMaxTransactionAmount(req.getMaxTransactionAmount());
        config.setUpdatedBy(adminId);
        PlatformPaymentConfig saved = platformRepo.save(config);
        log.info("[PaymentConfig] Platform config updated by adminId={}", adminId);
        return saved;
    }

    // ─── Community settings (community admin) ────────────────────────────────────

    @Transactional(readOnly = true)
    public CommunitySettings getCommunityPaymentSettings(Long communityId) {
        return communitySettingsRepo.findByCommunityId(communityId)
            .orElseThrow(() -> new IllegalArgumentException("No community settings for communityId=" + communityId));
    }

    @Transactional
    public void updateCommunityPaymentSettings(Long communityId, UpdateCommunityPaymentSettingsRequest req) {
        CommunitySettings settings = communitySettingsRepo.findByCommunityId(communityId)
            .orElseThrow(() -> new IllegalArgumentException("No community settings for communityId=" + communityId));

        if (req.getAllowedPaymentGateways() != null) {
            // Validate each requested gateway against the platform-enabled list so a community
            // cannot activate a gateway the platform operator has disabled.
            PlatformPaymentConfig platform = getPlatformConfig();
            List<String> platformEnabled = getEnabledGatewayNames(platform);
            List<String> invalid = req.getAllowedPaymentGateways().stream()
                    .filter(gw -> !platformEnabled.contains(gw))
                    .collect(Collectors.toList());
            if (!invalid.isEmpty()) {
                throw new PaymentGatewayNotAllowedException(
                        "Gateways not enabled on this platform: " + invalid);
            }
            settings.setAllowedPaymentGateways(req.getAllowedPaymentGateways().toArray(new String[0]));
        }
        if (req.getDefaultGateway() != null)    settings.setDefaultGateway(req.getDefaultGateway());
        if (req.getManualPaymentEnabled() != null) settings.setManualPaymentEnabled(req.getManualPaymentEnabled());
        if (req.getManualPaymentModes() != null) {
            settings.setManualPaymentModes(req.getManualPaymentModes().toArray(new String[0]));
        }
        if (req.getPaymentInstructions() != null) settings.setPaymentInstructions(req.getPaymentInstructions());
        if (req.getCashierCanRecordPayment() != null) settings.setCashierCanRecordPayment(req.getCashierCanRecordPayment());
        if (req.getFallbackGateway() != null)   settings.setFallbackGateway(req.getFallbackGateway());

        communitySettingsRepo.save(settings);
        log.info("[PaymentConfig] Community payment settings updated for communityId={}", communityId);
    }

    // ─── Gateway guard ────────────────────────────────────────────────────────────

    /**
     * Throws {@link PaymentGatewayNotAllowedException} if the given gateway is not
     * enabled for this community. Called by UnifiedPaymentOrderService before creating orders.
     */
    public void assertGatewayAllowed(Long communityId, String gateway) {
        CheckoutOptionsResponse opts = getCheckoutOptions(communityId);
        if (!opts.getGatewayOptions().contains(gateway)) {
            throw new PaymentGatewayNotAllowedException(
                "Gateway " + gateway + " is not enabled for community " + communityId);
        }
    }

    // ─── Internal ─────────────────────────────────────────────────────────────────

    private List<String> getEnabledGatewayNames(PlatformPaymentConfig config) {
        List<String> enabled = new ArrayList<>();
        if (config.isCashfreeEnabled()) enabled.add("CASHFREE");
        if (config.isRazorpayEnabled())  enabled.add("RAZORPAY");
        if (config.isManaPayEnabled())   enabled.add("MANA_PAY");
        if (config.isBbpsEnabled())      enabled.add("BBPS");
        return enabled;
    }
}
