package com.manacommunity.api.marketplace.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.marketplace.dto.MarketHandoverPassDto;
import com.manacommunity.api.marketplace.entity.MarketHandoverPass;
import com.manacommunity.api.marketplace.entity.MarketOrder;
import com.manacommunity.api.marketplace.repository.MarketHandoverPassRepository;
import com.manacommunity.api.marketplace.repository.MarketOrderRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarketHandoverSecurityService {

    private final MarketHandoverPassRepository passRepository;
    private final MarketOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public MarketHandoverPassDto generatePassForOrder(MarketOrder order) {
        // Generate 4-digit OTP PIN
        int pinInt = 1000 + random.nextInt(9000);
        String plainPin = String.valueOf(pinInt);
        String hashedPin = passwordEncoder.encode(plainPin);
        String qrCode = "GATE-PASS-" + order.getOrderNumber() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MarketHandoverPass pass = MarketHandoverPass.builder()
                .order(order)
                .passQrCode(qrCode)
                .otpPinHash(hashedPin)
                .status(MarketHandoverPass.PassStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        MarketHandoverPass saved = passRepository.save(pass);

        return MarketHandoverPassDto.builder()
                .id(saved.getId())
                .orderId(order.getId())
                .passQrCode(saved.getPassQrCode())
                .otpPin(plainPin) // plain PIN returned only upon creation
                .status(saved.getStatus())
                .expiresAt(saved.getExpiresAt())
                .build();
    }

    @Transactional
    public MarketHandoverPassDto verifyPassAtGate(MarketHandoverPassDto.VerifyRequest req, AppUser guard) {
        MarketHandoverPass pass = passRepository.findByPassQrCode(req.getPassQrCode())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid gate pass QR code: " + req.getPassQrCode()));

        if (pass.getStatus() != MarketHandoverPass.PassStatus.ACTIVE) {
            throw new InvalidInputException("Pass is no longer active (Status: " + pass.getStatus() + ")");
        }

        if (LocalDateTime.now().isAfter(pass.getExpiresAt())) {
            pass.setStatus(MarketHandoverPass.PassStatus.EXPIRED);
            passRepository.save(pass);
            throw new InvalidInputException("Gate pass has expired");
        }

        if (!passwordEncoder.matches(req.getPin(), pass.getOtpPinHash())) {
            throw new InvalidInputException("Invalid 4-digit security PIN");
        }

        pass.setStatus(MarketHandoverPass.PassStatus.VERIFIED);
        pass.setVerifiedAt(LocalDateTime.now());
        pass.setVerifiedByGuard(guard);

        MarketOrder order = pass.getOrder();
        if (order != null) {
            order.setStatus(MarketOrder.OrderStatus.DELIVERED);
            orderRepository.save(order);
        }

        MarketHandoverPass saved = passRepository.save(pass);

        return MarketHandoverPassDto.builder()
                .id(saved.getId())
                .orderId(order != null ? order.getId() : null)
                .passQrCode(saved.getPassQrCode())
                .status(saved.getStatus())
                .expiresAt(saved.getExpiresAt())
                .verifiedAt(saved.getVerifiedAt())
                .verifiedByGuardName(guard != null ? guard.getFullName() : "Security Gate")
                .build();
    }

    public MarketHandoverPassDto toDto(MarketHandoverPass p) {
        if (p == null) return null;
        return MarketHandoverPassDto.builder()
                .id(p.getId())
                .orderId(p.getOrder() != null ? p.getOrder().getId() : null)
                .passQrCode(p.getPassQrCode())
                .status(p.getStatus())
                .expiresAt(p.getExpiresAt())
                .verifiedAt(p.getVerifiedAt())
                .verifiedByGuardName(p.getVerifiedByGuard() != null ? p.getVerifiedByGuard().getFullName() : null)
                .build();
    }
}
