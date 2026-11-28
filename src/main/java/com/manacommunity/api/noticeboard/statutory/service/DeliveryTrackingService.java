package com.manacommunity.api.noticeboard.statutory.service;

import com.manacommunity.api.noticeboard.statutory.entity.*;
import com.manacommunity.api.noticeboard.statutory.entity.DeliveryChannel.*;
import com.manacommunity.api.noticeboard.statutory.entity.ReadReceipt.BeaconStatus;
import com.manacommunity.api.noticeboard.statutory.entity.StatutoryNotice.NoticeStatus;
import com.manacommunity.api.noticeboard.statutory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryTrackingService {

    private final StatutoryNoticeRepository noticeRepo;
    private final DeliveryChannelRepository channelRepo;
    private final ReadReceiptRepository receiptRepo;
    private final AudienceMemberRepository memberRepo;

    @Transactional
    public StatutoryNotice publishNotice(Long noticeId, List<ChannelType> channels) {
        StatutoryNotice notice = noticeRepo.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("Notice not found"));

        notice.setStatus(NoticeStatus.PUBLISHED);
        notice.setPublishedAt(LocalDateTime.now());

        List<AudienceMember> recipients = memberRepo.findByAudienceGroupId(notice.getAudienceGroupId());
        notice.setTotalRecipients(recipients.size());

        for (ChannelType channelType : channels) {
            DeliveryChannel channel = DeliveryChannel.builder()
                    .noticeId(noticeId).communityId(notice.getCommunityId())
                    .channelType(channelType).channelStatus(ChannelStatus.DISPATCHING)
                    .totalSent(recipients.size()).dispatchStartedAt(LocalDateTime.now())
                    .build();
            channelRepo.save(channel);

            List<ReadReceipt> batch = new ArrayList<>(Math.min(recipients.size(), 500));
            LocalDateTime now = LocalDateTime.now();
            for (AudienceMember member : recipients) {
                batch.add(ReadReceipt.builder()
                        .noticeId(noticeId).communityId(notice.getCommunityId())
                        .flatNumber(member.getFlatNumber()).towerName(member.getTowerName())
                        .recipientName(member.getResidentName())
                        .recipientEmail(member.getEmail()).recipientPhone(member.getPhone())
                        .recipientUserId(member.getUserId())
                        .beaconStatus(BeaconStatus.SENT).sentAt(now)
                        .deliveredVia(channelType)
                        .beaconTrackingId(UUID.randomUUID().toString())
                        .legallyValid(notice.isStatutory())
                        .build());
                if (batch.size() >= 500) {
                    receiptRepo.saveAll(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                receiptRepo.saveAll(batch);
            }

            channel.setChannelStatus(ChannelStatus.COMPLETED);
            channel.setDispatchCompletedAt(LocalDateTime.now());
            channel.setTotalDelivered(recipients.size());
            channel.setDeliveryRatePercent(100.0);
            channelRepo.save(channel);
        }

        return noticeRepo.save(notice);
    }

    @Transactional
    public ReadReceipt processBeacon(String trackingId, BeaconStatus newStatus) {
        ReadReceipt receipt = receiptRepo.findByBeaconTrackingId(trackingId)
                .orElseThrow(() -> new RuntimeException("Beacon not found: " + trackingId));

        switch (newStatus) {
            case DELIVERED -> receipt.setDeliveredAt(LocalDateTime.now());
            case OPENED -> receipt.setOpenedAt(LocalDateTime.now());
            case ACKNOWLEDGED -> receipt.setAcknowledgedAt(LocalDateTime.now());
            default -> {}
        }
        receipt.setBeaconStatus(newStatus);

        updateNoticeCounters(receipt.getNoticeId());
        return receiptRepo.save(receipt);
    }

    private void updateNoticeCounters(Long noticeId) {
        List<ReadReceipt> all = receiptRepo.findByNoticeId(noticeId);
        StatutoryNotice notice = noticeRepo.findById(noticeId).orElse(null);
        if (notice == null) return;

        notice.setDeliveredCount((int) all.stream()
                .filter(r -> r.getBeaconStatus().ordinal() >= BeaconStatus.DELIVERED.ordinal()).count());
        notice.setOpenedCount((int) all.stream()
                .filter(r -> r.getBeaconStatus().ordinal() >= BeaconStatus.OPENED.ordinal()).count());
        noticeRepo.save(notice);
    }

    public NoticeDeliveryDashboard getDeliveryDashboard(Long noticeId) {
        StatutoryNotice notice = noticeRepo.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("Notice not found"));

        List<ReadReceipt> receipts = receiptRepo.findByNoticeId(noticeId);
        List<DeliveryChannel> channels = channelRepo.findByNoticeId(noticeId);

        Map<String, Long> statusCounts = receipts.stream()
                .collect(Collectors.groupingBy(r -> r.getBeaconStatus().name(), Collectors.counting()));

        Map<String, Map<String, Long>> towerWise = receipts.stream()
                .collect(Collectors.groupingBy(ReadReceipt::getTowerName,
                        Collectors.groupingBy(r -> r.getBeaconStatus().name(), Collectors.counting())));

        List<ReadReceipt> pending = receipts.stream()
                .filter(r -> r.getBeaconStatus() == BeaconStatus.SENT || r.getBeaconStatus() == BeaconStatus.DELIVERED)
                .toList();

        double openRate = receipts.isEmpty() ? 0 :
                (receipts.stream().filter(r -> r.getBeaconStatus().ordinal() >= BeaconStatus.OPENED.ordinal()).count() * 100.0) / receipts.size();

        return new NoticeDeliveryDashboard(notice, channels, receipts.size(),
                statusCounts, towerWise, pending.size(), Math.round(openRate * 100.0) / 100.0, receipts);
    }

    public record NoticeDeliveryDashboard(StatutoryNotice notice, List<DeliveryChannel> channels,
                                            int totalRecipients, Map<String, Long> statusBreakdown,
                                            Map<String, Map<String, Long>> towerWiseDelivery,
                                            int pendingCount, double openRatePercent,
                                            List<ReadReceipt> receipts) {}
}
