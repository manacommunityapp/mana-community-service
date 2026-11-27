package com.manacommunity.api.noticeboard.statutory.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_read_receipts", indexes = {
    @Index(name = "idx_receipt_notice_flat", columnList = "noticeId, flatNumber"),
    @Index(name = "idx_receipt_beacon_batch", columnList = "noticeId, beaconStatus")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReadReceipt extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long noticeId;
    private String communityId;

    private String flatNumber;
    private String towerName;
    private String recipientName;
    private String recipientEmail;
    private String recipientPhone;
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    private BeaconStatus beaconStatus;

    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private LocalDateTime acknowledgedAt;

    @Enumerated(EnumType.STRING)
    private DeliveryChannel.ChannelType deliveredVia;

    private String deviceInfo;
    private String ipAddress;
    private String beaconTrackingId;

    private boolean legallyValid;
    private String signatureHash;

    public enum BeaconStatus { QUEUED, SENT, DELIVERED, OPENED, ACKNOWLEDGED, BOUNCED, FAILED }
}
