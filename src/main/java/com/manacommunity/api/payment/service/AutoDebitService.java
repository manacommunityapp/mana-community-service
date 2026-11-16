package com.manacommunity.api.payment.service;

import com.manacommunity.api.payment.dto.SetupMandateRequest;
import com.manacommunity.api.payment.entity.BankMandate;
import com.manacommunity.api.payment.repository.BankMandateRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AutoDebitService {

    private final BankMandateRepository mandateRepository;

    public Optional<BankMandate> getMandate(AppUser user) {
        return mandateRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
    }

    public BankMandate setupMandate(SetupMandateRequest req, AppUser user) {
        mandateRepository.findByUserIdAndStatus(user.getId(), "ACTIVE")
                .ifPresent(existing -> {
                    existing.setStatus("CANCELLED");
                    mandateRepository.save(existing);
                });
        BankMandate mandate = BankMandate.builder()
                .user(user)
                .community(user.getCommunity())
                .bankName(req.getBankName())
                .accountNumber(req.getAccountNumber())
                .ifsc(req.getIfsc())
                .accountHolderName(req.getAccountHolderName())
                .monthlyAmount(req.getMonthlyAmount())
                .mandateRef("MND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status("ACTIVE")
                .build();
        return mandateRepository.save(mandate);
    }

    public void cancelMandate(AppUser user) {
        mandateRepository.findByUserIdAndStatus(user.getId(), "ACTIVE")
                .ifPresent(mandate -> {
                    mandate.setStatus("CANCELLED");
                    mandateRepository.save(mandate);
                });
    }
}
