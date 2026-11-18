package com.manacommunity.api.trip.p2p.service;

import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.payments.config.RazorpayProperties;
import com.manacommunity.api.payments.config.entity.PlatformPaymentConfig;
import com.manacommunity.api.payments.config.repository.PlatformPaymentConfigRepository;
import com.manacommunity.api.payments.exception.PaymentGatewayException;
import com.manacommunity.api.trip.entity.Trip;
import com.manacommunity.api.trip.p2p.dto.TripP2PDtos.CreateP2PDebtRequest;
import com.manacommunity.api.trip.p2p.entity.TripP2PCollectRequest;
import com.manacommunity.api.trip.p2p.entity.TripP2PCollectStatus;
import com.manacommunity.api.trip.p2p.repository.TripP2PCollectRequestRepository;
import com.manacommunity.api.trip.repository.TripRepository;
import com.manacommunity.api.trip.split.service.TripSplitAccess;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Handles peer-to-peer UPI Collect requests within a community trip.
 *
 * <p>Financial trust: amount is stored when the debt record is created by the
 * creditor/organizer — the initiateCollect call uses the stored amount, never a
 * client-supplied value.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripP2PCollectService {

    private final TripP2PCollectRequestRepository collectRepo;
    private final TripRepository tripRepo;
    private final TripSplitAccess tripSplitAccess;
    private final PlatformPaymentConfigRepository platformConfigRepo;
    private final RazorpayProperties razorpayProperties;
    private final Optional<RazorpayClient> razorpayClient;

    // ── Debt creation ────────────────────────────────────────────────────────

    /**
     * Creates a debt record. The creditor (trip organizer or community admin)
     * specifies who owes what. The debtor's UPI VPA is collected later.
     *
     * <p>Amount is taken from the request and stored. It is NOT recalculated
     * on collect initiation — the stored value is authoritative.
     */
    @Transactional
    public TripP2PCollectRequest createDebt(String tripId, Long creditorUserId,
                                             CreateP2PDebtRequest req, Long communityId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip", tripId));

        if (!trip.getCommunityId().equals(communityId)) {
            throw new UnauthorizedActionException("Trip does not belong to your community");
        }

        if (!tripSplitAccess.isOrganizer(trip, creditorUserId)) {
            throw new UnauthorizedActionException("Only the trip organizer can create debt records");
        }

        BigDecimal amount = req.getAmount();

        // Cap at platform max transaction amount if configured
        Optional<PlatformPaymentConfig> platformConfig = platformConfigRepo.findFirstByOrderByIdAsc();
        platformConfig.ifPresent(cfg -> {
            if (cfg.getMaxTransactionAmount() != null
                    && amount.compareTo(cfg.getMaxTransactionAmount()) > 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Amount exceeds platform transaction limit of ₹" + cfg.getMaxTransactionAmount());
            }
        });

        TripP2PCollectRequest record = TripP2PCollectRequest.builder()
                .communityId(communityId)
                .tripId(tripId)
                .creditorUserId(creditorUserId)
                .debtorUserId(req.getDebtorUserId())
                .amount(amount)
                .description(req.getDescription())
                .status(TripP2PCollectStatus.PENDING_VPA)
                .build();

        return collectRepo.save(record);
    }

    // ── Collect initiation ───────────────────────────────────────────────────

    /**
     * Initiates a UPI Collect request via Razorpay.
     * Uses the stored amount — never recalculated from client input.
     */
    @Transactional
    public TripP2PCollectRequest initiateCollect(Long collectId, String upiVpa, Long initiatorUserId) {
        TripP2PCollectRequest record = collectRepo.findById(collectId)
                .orElseThrow(() -> new ResourceNotFoundException("P2P collect request", collectId));

        if (record.getStatus() != TripP2PCollectStatus.PENDING_VPA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Collect request is already in status: " + record.getStatus());
        }

        if (!record.getCreditorUserId().equals(initiatorUserId)) {
            throw new UnauthorizedActionException("Only the creditor can initiate a collect request");
        }

        if (!razorpayProperties.isEnabled()) {
            throw new PaymentGatewayException(
                    "Razorpay is not enabled — configure RAZORPAY_ENABLED=true to use UPI Collect");
        }

        RazorpayClient client = razorpayClient.orElseThrow(() ->
                new PaymentGatewayException("Razorpay client not initialised — check credentials"));

        // Amount stored server-side — amount is in rupees, Razorpay expects paise
        long amountPaise = record.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String razorpayPaymentId;
        try {
            JSONObject paymentRequest = new JSONObject();
            paymentRequest.put("amount", amountPaise);
            paymentRequest.put("currency", "INR");
            paymentRequest.put("description",
                    record.getDescription() != null ? record.getDescription() : "Trip settlement");

            JSONObject upi = new JSONObject();
            upi.put("flow", "collect");
            upi.put("vpa", upiVpa);
            paymentRequest.put("upi", upi);

            Payment payment = client.payments.createUpi(paymentRequest);
            razorpayPaymentId = payment.get("razorpay_payment_id");

            log.info("[TripP2P] UPI Collect initiated collectId={} debtorVpa={} razorpayPaymentId={}",
                    collectId, upiVpa, razorpayPaymentId);
        } catch (RazorpayException e) {
            log.error("[TripP2P] Razorpay collect initiation failed collectId={}: {}", collectId, e.getMessage());
            throw new PaymentGatewayException("UPI Collect initiation failed: " + e.getMessage(), e);
        }

        record.setUpiVpa(upiVpa);
        record.setRazorpayPaymentId(razorpayPaymentId);
        record.setStatus(TripP2PCollectStatus.COLLECT_INITIATED);
        record.setInitiatedAt(LocalDateTime.now());

        return collectRepo.save(record);
    }

    // ── Webhook callback ─────────────────────────────────────────────────────

    /**
     * Called from the Razorpay webhook handler when a UPI collect payment is
     * captured or failed. Idempotent — safe to call multiple times with the same
     * paymentId (no-op if already in a terminal status).
     */
    @Transactional
    public void handleCollectCallback(String razorpayPaymentId, String eventName) {
        Optional<TripP2PCollectRequest> opt = collectRepo.findByRazorpayPaymentId(razorpayPaymentId);
        if (opt.isEmpty()) {
            return; // not a P2P collect — normal payment order path handles it
        }

        TripP2PCollectRequest record = opt.get();
        if (record.getStatus() == TripP2PCollectStatus.COLLECT_APPROVED
                || record.getStatus() == TripP2PCollectStatus.COLLECT_REJECTED) {
            log.debug("[TripP2P] Duplicate webhook ignored for already-terminal collectId={}", record.getId());
            return;
        }

        if ("payment.captured".equals(eventName) || "order.paid".equals(eventName)) {
            record.setStatus(TripP2PCollectStatus.COLLECT_APPROVED);
            record.setSettledAt(LocalDateTime.now());
            log.info("[TripP2P] Collect APPROVED collectId={} paymentId={}", record.getId(), razorpayPaymentId);
        } else if ("payment.failed".equals(eventName)) {
            record.setStatus(TripP2PCollectStatus.COLLECT_REJECTED);
            log.info("[TripP2P] Collect REJECTED collectId={} paymentId={}", record.getId(), razorpayPaymentId);
        } else {
            log.debug("[TripP2P] Unhandled event {} for collectId={}", eventName, record.getId());
            return;
        }

        collectRepo.save(record);
    }

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<TripP2PCollectRequest> listForTrip(String tripId, Long requestorUserId, boolean isOrganizerOrAdmin) {
        List<TripP2PCollectRequest> all = collectRepo.findByTripId(tripId);
        if (isOrganizerOrAdmin) {
            return all;
        }
        // Participants see only records where they are the debtor or creditor
        return all.stream()
                .filter(r -> r.getDebtorUserId().equals(requestorUserId)
                        || r.getCreditorUserId().equals(requestorUserId))
                .toList();
    }

    // ── Cancellation ─────────────────────────────────────────────────────────

    @Transactional
    public void cancelDebt(Long collectId, Long requestorUserId, boolean isOrganizerOrAdmin) {
        TripP2PCollectRequest record = collectRepo.findById(collectId)
                .orElseThrow(() -> new ResourceNotFoundException("P2P collect request", collectId));

        boolean isDebtor = record.getDebtorUserId().equals(requestorUserId);
        boolean isCreditor = record.getCreditorUserId().equals(requestorUserId);

        if (!isOrganizerOrAdmin && !isDebtor && !isCreditor) {
            throw new UnauthorizedActionException("Only the debtor, creditor, or admin can cancel a collect request");
        }

        if (record.getStatus() == TripP2PCollectStatus.COLLECT_APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot cancel an already-approved collect request");
        }

        record.setStatus(TripP2PCollectStatus.CANCELLED);
        collectRepo.save(record);
        log.info("[TripP2P] Collect CANCELLED collectId={} by userId={}", collectId, requestorUserId);
    }
}
