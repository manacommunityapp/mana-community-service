package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.GroupEnquiryRequest;
import com.manacommunity.api.insurance.dto.InsuranceDtos.GroupEnquiryView;
import com.manacommunity.api.insurance.entity.GroupEnquiry;
import com.manacommunity.api.insurance.entity.InsuranceEnums.ConsentPurpose;
import com.manacommunity.api.insurance.repository.GroupEnquiryRepository;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.util.List;

/** Group-insurance enquiries. An enquiry is a lead only: no rates or benefits are promised. */
@Service
@RequiredArgsConstructor
public class GroupEnquiryService {

    public static final String NOTICE =
            "This is an enquiry only. Group rates, eligibility and benefits are not guaranteed and depend on "
          + "written confirmation from an authorised insurance partner.";

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    private final GroupEnquiryRepository enquiries;
    private final InsuranceConsentService consents;
    private final InsuranceAuditService audit;

    @Transactional
    public GroupEnquiryView create(AppUser user, GroupEnquiryRequest r) {
        consents.requireActive(user.getId(), r.consentId(), ConsentPurpose.GROUP_ENQUIRY);
        StringBuilder ref = new StringBuilder("MGE-");
        for (int i = 0; i < 8; i++) ref.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        GroupEnquiry e = enquiries.save(GroupEnquiry.builder()
                .enquiryRef(ref.toString()).userId(user.getId())
                .communityId(user.getCommunity() == null ? null : user.getCommunity().getId())
                .organisationName(r.organisationName().trim()).organisationType(r.organisationType())
                .coverageScope(r.coverageScope()).coverageSought(r.coverageSought())
                .approxMembers(r.approxMembers()).requirements(r.requirements())
                .contactMethod(r.contactMethod()).consentId(r.consentId()).build());
        audit.record(user.getId(), "GROUP_ENQUIRY_CREATED", "GROUP_ENQUIRY", String.valueOf(e.getId()),
                "scope=" + e.getCoverageScope());
        return view(e);
    }

    public List<GroupEnquiryView> list(Long userId) {
        return enquiries.findByUserIdOrderByCreatedAtDesc(userId).stream().map(GroupEnquiryService::view).toList();
    }

    public GroupEnquiryView get(Long userId, Long id) {
        return view(enquiries.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ManaCommunityException("Enquiry not found", HttpStatus.NOT_FOUND, "NOT_FOUND")));
    }

    static GroupEnquiryView view(GroupEnquiry e) {
        return new GroupEnquiryView(e.getId(), e.getEnquiryRef(), e.getOrganisationName(), e.getOrganisationType(),
                e.getCoverageScope().name(), e.getCoverageSought(), e.getApproxMembers(), e.getStatus().name(),
                e.getCreatedAt(), NOTICE);
    }
}
