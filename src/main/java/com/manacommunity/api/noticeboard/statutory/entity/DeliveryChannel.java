package com.manacommunity.api.noticeboard.statutory.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notice_delivery_channels")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryChannel extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long noticeId;
    private String communityId;

    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    private ChannelStatus channelStatus;

    private int totalSent;
    private int totalDelivered;
    private int totalOpened;
    private int totalFailed;
    private int totalBounced;

    private LocalDateTime dispatchStartedAt;
    private LocalDateTime dispatchCompletedAt;

    private double deliveryRatePercent;
    private double openRatePercent;

    private String providerRef;
    private String errorSummary;

    public enum ChannelType { APP_PUSH, EMAIL, WHATSAPP, SMS, IN_APP }
    public enum ChannelStatus { PENDING, DISPATCHING, COMPLETED, PARTIAL_FAILURE, FAILED }
}
