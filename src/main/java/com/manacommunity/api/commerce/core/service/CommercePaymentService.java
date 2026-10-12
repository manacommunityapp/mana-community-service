package com.manacommunity.api.commerce.core.service;

import com.manacommunity.api.commerce.core.dto.CommerceOrderDto;
import com.manacommunity.api.commerce.core.dto.CommercePaymentInitiateResponse;
import com.manacommunity.api.commerce.core.dto.CommercePaymentVerifyRequest;
import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.commerce.core.model.CommerceOrderStatus;
import com.manacommunity.api.commerce.core.model.CommercePayment;
import com.manacommunity.api.commerce.core.repository.CommerceOrderRepository;
import com.manacommunity.api.commerce.core.repository.CommercePaymentRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommercePaymentService {

    private final CommerceOrderRepository orderRepository;
    private final CommercePaymentRepository paymentRepository;
    private final CommerceCoreService coreService;
    private final CommerceInventoryService inventoryService;
    private final CommerceNotificationService notificationService;

    @Value("${app.razorpay.key-id:rzp_test_placeholder}")
    private String razorpayKeyId;

    @Value("${app.razorpay.key-secret:placeholder_secret}")
    private String razorpayKeySecret;

    @Value("${app.razorpay.enabled:false}")
    private boolean razorpayEnabled;

    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    /**
     * Step 1: Creates a Razorpay order and a pending CommercePayment record.
     * Returns the razorpayOrderId and keyId so the frontend can open the checkout modal.
     */
    @Transactional
    public CommercePaymentInitiateResponse initiatePayment(AppUser user, String orderNumber) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new SecurityException("Not your order");
        }

        if (!CommerceOrderStatus.PENDING.equals(order.getStatus())) {
            throw new IllegalStateException("Order is not in PENDING state: " + order.getStatus());
        }

        // Amount in paise (Razorpay uses smallest currency unit)
        long amountPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String razorpayOrderId;
        if (razorpayEnabled) {
            razorpayOrderId = createRazorpayOrder(amountPaise, order.getOrderNumber());
        } else {
            // Mock for dev/test — use a recognizable prefix
            razorpayOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        // Persist pending payment record (idempotent — reuse if already initiated for this order)
        CommercePayment payment = paymentRepository.findByOrderId(order.getId())
                .orElse(null);

        if (payment == null) {
            payment = CommercePayment.builder()
                    .order(order)
                    .paymentRef("CPAY-" + orderNumber + "-" + System.currentTimeMillis())
                    .gateway("RAZORPAY")
                    .paymentMethod("UPI")
                    .amount(order.getTotalAmount())
                    .currency("INR")
                    .status("INITIATED")
                    .razorpayOrderId(razorpayOrderId)
                    .build();
        } else {
            payment.setRazorpayOrderId(razorpayOrderId);
            payment.setStatus("INITIATED");
        }
        paymentRepository.save(payment);

        return CommercePaymentInitiateResponse.builder()
                .orderNumber(orderNumber)
                .razorpayOrderId(razorpayOrderId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .keyId(razorpayKeyId)
                .description("Order " + orderNumber + " · " + order.getItems().size() + " item(s)")
                .build();
    }

    /**
     * Step 2: Verifies Razorpay HMAC-SHA256 signature, marks payment CAPTURED,
     * and transitions the commerce order to PAYMENT_AUTHORIZED.
     */
    @Transactional
    public CommerceOrderDto verifyAndCapture(AppUser user, String orderNumber, CommercePaymentVerifyRequest req) {
        CommerceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderNumber));

        if (!order.getBuyer().getId().equals(user.getId())) {
            throw new SecurityException("Not your order");
        }

        CommercePayment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("No payment found for order: " + orderNumber));

        // Verify HMAC-SHA256 signature: sign(razorpayOrderId + "|" + razorpayPaymentId)
        if (!verifySignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature())) {
            payment.setStatus("FAILED");
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw new SecurityException("Payment signature verification failed");
        }

        payment.setRazorpayPaymentId(req.getRazorpayPaymentId());
        payment.setGatewaySignature(req.getRazorpaySignature());
        payment.setStatus("CAPTURED");
        payment.setCapturedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Transition order: PENDING → PAYMENT_AUTHORIZED → CONFIRMED
        order.setStatus(CommerceOrderStatus.PAYMENT_AUTHORIZED);
        order.setPaymentStatus("PAID");
        orderRepository.save(order);

        // Phase 8: commit inventory stock on successful payment
        try {
            inventoryService.commitStock(order);
        } catch (Exception e) {
            log.warn("Inventory commit failed after payment for order {}: {}", orderNumber, e.getMessage());
        }

        notificationService.notifyPaymentReceived(order);
        log.info("Commerce payment captured: order={} razorpayPaymentId={}", orderNumber, req.getRazorpayPaymentId());

        return coreService.getOrderByNumber(user, orderNumber);
    }

    private String createRazorpayOrder(long amountPaise, String receiptRef) {
        RestTemplate rest = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Basic auth: keyId:keySecret as Base64
        String credentials = Base64.getEncoder().encodeToString(
                (razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + credentials);

        Map<String, Object> body = Map.of(
                "amount", amountPaise,
                "currency", "INR",
                "receipt", receiptRef,
                "payment_capture", 1
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = rest.postForObject(
                RAZORPAY_ORDERS_URL,
                new HttpEntity<>(body, headers),
                Map.class);

        if (response == null || !response.containsKey("id")) {
            throw new IllegalStateException("Failed to create Razorpay order");
        }
        return (String) response.get("id");
    }

    private boolean verifySignature(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computed);
            return computedHex.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC verification error", e);
            return false;
        }
    }
}
