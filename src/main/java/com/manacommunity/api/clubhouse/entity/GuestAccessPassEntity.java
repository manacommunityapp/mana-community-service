package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "guest_access_passes", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class GuestAccessPassEntity {

    @Id
    @Column(name = "pass_id", nullable = false)
    private UUID passId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "facility_code", nullable = false, length = 40)
    private String facilityCode;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "guest_name", nullable = false, length = 140)
    private String guestName;

    @Column(name = "guest_phone", length = 24)
    private String guestPhone;

    @Column(name = "pricing_tier", nullable = false, length = 30)
    private String pricingTier;

    @Column(name = "unit_price_paise", nullable = false)
    private long unitPricePaise;

    @Column(name = "gst_paise", nullable = false)
    private long gstPaise = 0L;

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Column(name = "jti", nullable = false, length = 64, unique = true)
    private String jti;

    @Column(name = "qr_token", nullable = false, columnDefinition = "text")
    private String qrToken;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "entry_count", nullable = false)
    private int entryCount = 0;

    @Column(name = "max_entries", nullable = false)
    private int maxEntries = 1;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until", nullable = false)
    private Instant validUntil;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
