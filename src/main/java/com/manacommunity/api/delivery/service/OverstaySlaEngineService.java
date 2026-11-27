package com.manacommunity.api.delivery.service;

import com.manacommunity.api.delivery.model.DeliveryPartner;
import com.manacommunity.api.delivery.model.ExpressDeliveryManifest;
import com.manacommunity.api.delivery.model.ExpressDeliveryPass;
import com.manacommunity.api.delivery.model.ExpressPassStatus;
import com.manacommunity.api.visitor.entity.VisitorPass;
import lombok.Builder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Category-aware stateful overstay & anomaly evaluation engine.
 * Eliminates false positives for pre-approved home-service technicians while
 * catching quick-commerce and courier overstays promptly.
 */
@Service
public class OverstaySlaEngineService {

    public static final int QUICK_COMMERCE_SLA_MINUTES = 45;
    public static final int COURIER_BASE_SLA_MINUTES = 15;
    public static final int COURIER_PER_FLAT_MINUTES = 3;
    public static final int SERVICE_TECH_SOFT_PING_MINUTES = 180;
    public static final int SERVICE_TECH_HARD_ESCALATION_MINUTES = 300;
    public static final int DEFAULT_VISITOR_SLA_MINUTES = 720;

    public enum OverstayAction {
        NONE,
        RESIDENT_CONFIRMATION_PING,
        SECURITY_SUPERVISOR_ALERT
    }

    @Builder
    public record OverstayEvaluation(
            boolean overstayed,
            OverstayAction action,
            long elapsedMinutes,
            int slaLimitMinutes,
            String category,
            String reason
    ) {}

    public int resolveCourierSlaMinutes(int distinctFlats) {
        int flats = Math.max(1, distinctFlats);
        return COURIER_BASE_SLA_MINUTES + (COURIER_PER_FLAT_MINUTES * flats);
    }

    public OverstayEvaluation evaluateExpressPass(ExpressDeliveryPass pass, LocalDateTime now) {
        if (pass == null || pass.getStatus() != ExpressPassStatus.CHECKED_IN || pass.getCheckedInAt() == null) {
            return OverstayEvaluation.builder()
                    .overstayed(false)
                    .action(OverstayAction.NONE)
                    .elapsedMinutes(0)
                    .slaLimitMinutes(QUICK_COMMERCE_SLA_MINUTES)
                    .category("QUICK_COMMERCE")
                    .reason("Not checked in")
                    .build();
        }
        long elapsed = Math.max(0, Duration.between(pass.getCheckedInAt(), now).toMinutes());
        int sla = isQuickCommerce(pass.getPartner()) ? QUICK_COMMERCE_SLA_MINUTES : resolveCourierSlaMinutes(1);
        String category = isQuickCommerce(pass.getPartner()) ? "QUICK_COMMERCE" : "COURIER";

        if (elapsed >= sla) {
            return OverstayEvaluation.builder()
                    .overstayed(true)
                    .action(OverstayAction.SECURITY_SUPERVISOR_ALERT)
                    .elapsedMinutes(elapsed)
                    .slaLimitMinutes(sla)
                    .category(category)
                    .reason(String.format("%s rider (%s) exceeded %d-min gate SLA (inside for %d mins)",
                            pass.getPartner(), pass.getRiderName(), sla, elapsed))
                    .build();
        }
        return OverstayEvaluation.builder()
                .overstayed(false)
                .action(OverstayAction.NONE)
                .elapsedMinutes(elapsed)
                .slaLimitMinutes(sla)
                .category(category)
                .reason("Within SLA")
                .build();
    }

    public OverstayEvaluation evaluateManifest(ExpressDeliveryManifest manifest, LocalDateTime now) {
        if (manifest == null || manifest.getStatus() != ExpressPassStatus.CHECKED_IN || manifest.getCheckedInAt() == null) {
            return OverstayEvaluation.builder()
                    .overstayed(false)
                    .action(OverstayAction.NONE)
                    .elapsedMinutes(0)
                    .slaLimitMinutes(COURIER_BASE_SLA_MINUTES)
                    .category("BULK_COURIER")
                    .reason("Manifest not checked in")
                    .build();
        }
        long elapsed = Math.max(0, Duration.between(manifest.getCheckedInAt(), now).toMinutes());
        int flats = manifest.getDistinctFlats() != null ? manifest.getDistinctFlats() : 1;
        int sla = isQuickCommerce(manifest.getPartner()) && flats <= 1
                ? QUICK_COMMERCE_SLA_MINUTES
                : resolveCourierSlaMinutes(flats);

        if (elapsed >= sla) {
            return OverstayEvaluation.builder()
                    .overstayed(true)
                    .action(OverstayAction.SECURITY_SUPERVISOR_ALERT)
                    .elapsedMinutes(elapsed)
                    .slaLimitMinutes(sla)
                    .category("BULK_COURIER")
                    .reason(String.format("Bulk courier manifest %s (%d flats) exceeded %d-min dynamic SLA (%d mins elapsed)",
                            manifest.getManifestCode(), flats, sla, elapsed))
                    .build();
        }
        return OverstayEvaluation.builder()
                .overstayed(false)
                .action(OverstayAction.NONE)
                .elapsedMinutes(elapsed)
                .slaLimitMinutes(sla)
                .category("BULK_COURIER")
                .reason("Within dynamic courier SLA")
                .build();
    }

    public OverstayEvaluation evaluateVisitorPass(VisitorPass pass, LocalDateTime now) {
        if (pass == null || pass.getStatus() != VisitorPass.PassStatus.CHECKED_IN || pass.getCheckedInAt() == null) {
            return OverstayEvaluation.builder()
                    .overstayed(false)
                    .action(OverstayAction.NONE)
                    .elapsedMinutes(0)
                    .slaLimitMinutes(DEFAULT_VISITOR_SLA_MINUTES)
                    .category("VISITOR")
                    .reason("Not checked in")
                    .build();
        }
        long elapsed = Math.max(0, Duration.between(pass.getCheckedInAt(), now).toMinutes());
        VisitorPass.PassType type = pass.getPassType();

        if (isServiceTechnicianType(type)) {
            if (elapsed >= SERVICE_TECH_HARD_ESCALATION_MINUTES) {
                return OverstayEvaluation.builder()
                        .overstayed(true)
                        .action(OverstayAction.SECURITY_SUPERVISOR_ALERT)
                        .elapsedMinutes(elapsed)
                        .slaLimitMinutes(SERVICE_TECH_HARD_ESCALATION_MINUTES)
                        .category("SERVICE_TECH")
                        .reason(String.format("Service technician %s inside for %d mins without resident extension",
                                pass.getVisitorName(), elapsed))
                        .build();
            }
            if (elapsed >= SERVICE_TECH_SOFT_PING_MINUTES) {
                return OverstayEvaluation.builder()
                        .overstayed(true)
                        .action(OverstayAction.RESIDENT_CONFIRMATION_PING)
                        .elapsedMinutes(elapsed)
                        .slaLimitMinutes(SERVICE_TECH_SOFT_PING_MINUTES)
                        .category("SERVICE_TECH")
                        .reason(String.format("Soft check-in ping: pre-approved %s has been on-site for %d mins — confirm work ongoing",
                                type != null ? type.name() : "SERVICE", elapsed))
                        .build();
            }
            return OverstayEvaluation.builder()
                    .overstayed(false)
                    .action(OverstayAction.NONE)
                    .elapsedMinutes(elapsed)
                    .slaLimitMinutes(SERVICE_TECH_SOFT_PING_MINUTES)
                    .category("SERVICE_TECH")
                    .reason("Pre-approved service technician within 180-min work window")
                    .build();
        }

        if (type == VisitorPass.PassType.DELIVERY || type == VisitorPass.PassType.COURIER) {
            int sla = type == VisitorPass.PassType.DELIVERY ? QUICK_COMMERCE_SLA_MINUTES : resolveCourierSlaMinutes(3);
            if (elapsed >= sla) {
                return OverstayEvaluation.builder()
                        .overstayed(true)
                        .action(OverstayAction.SECURITY_SUPERVISOR_ALERT)
                        .elapsedMinutes(elapsed)
                        .slaLimitMinutes(sla)
                        .category("DELIVERY")
                        .reason(String.format("Delivery/Courier visitor %s exceeded %d-min SLA (%d mins inside)",
                                pass.getVisitorName(), sla, elapsed))
                        .build();
            }
            return OverstayEvaluation.builder()
                    .overstayed(false)
                    .action(OverstayAction.NONE)
                    .elapsedMinutes(elapsed)
                    .slaLimitMinutes(sla)
                    .category("DELIVERY")
                    .reason("Within delivery SLA")
                    .build();
        }

        if (elapsed >= DEFAULT_VISITOR_SLA_MINUTES) {
            return OverstayEvaluation.builder()
                    .overstayed(true)
                    .action(OverstayAction.SECURITY_SUPERVISOR_ALERT)
                    .elapsedMinutes(elapsed)
                    .slaLimitMinutes(DEFAULT_VISITOR_SLA_MINUTES)
                    .category("VISITOR")
                    .reason("Automated check: visitor checked in > 12 hours ago without check-out record.")
                    .build();
        }
        return OverstayEvaluation.builder()
                .overstayed(false)
                .action(OverstayAction.NONE)
                .elapsedMinutes(elapsed)
                .slaLimitMinutes(DEFAULT_VISITOR_SLA_MINUTES)
                .category("VISITOR")
                .reason("Within standard visitor window")
                .build();
    }

    private boolean isQuickCommerce(DeliveryPartner partner) {
        return partner == DeliveryPartner.BLINKIT
                || partner == DeliveryPartner.ZEPTO
                || partner == DeliveryPartner.SWIGGY
                || partner == DeliveryPartner.ZOMATO;
    }

    private boolean isServiceTechnicianType(VisitorPass.PassType type) {
        return type == VisitorPass.PassType.SERVICE
                || type == VisitorPass.PassType.CONTRACTOR
                || type == VisitorPass.PassType.STAFF
                || type == VisitorPass.PassType.MAID
                || type == VisitorPass.PassType.VENDOR;
    }
}
