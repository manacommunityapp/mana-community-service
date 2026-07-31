package com.cpn.application.referrals;

import com.cpn.domain.referrals.model.Referral;
import com.cpn.domain.referrals.repository.ReferralRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReferralsService {

    private final ReferralRepository referralRepository;

    @Transactional(readOnly = true)
    public List<Referral> getAllOpenReferrals() {
        return referralRepository.findByStatus("OPEN");
    }

    @Transactional
    public Referral createReferral(Referral referral) {
        referral.setStatus("OPEN");
        return referralRepository.save(referral);
    }
}
