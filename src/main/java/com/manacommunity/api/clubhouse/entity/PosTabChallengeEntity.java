package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_tab_challenges", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class PosTabChallengeEntity {

    @Id
    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "unit_code", nullable = false, length = 40)
    private String unitCode;

    @Column(name = "terminal_id", nullable = false, length = 60)
    private String terminalId;

    @Column(name = "outlet_name", nullable = false, length = 120)
    private String outletName;

    @Column(name = "client_tx_uuid", nullable = false, length = 100)
    private String clientTxUuid;

    @Column(name = "subtotal_paise", nullable = false)
    private long subtotalPaise;

    @Column(name = "gst_paise", nullable = false)
    private long gstPaise;

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Column(name = "items_summary", nullable = false, length = 400)
    private String itemsSummary;

    @Column(name = "auth_method", nullable = false, length = 30)
    private String authMethod;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
