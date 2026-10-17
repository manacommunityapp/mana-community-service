package com.manacommunity.api.insurance.service;

import com.manacommunity.api.insurance.dto.InsuranceDtos.CatalogueItem;
import com.manacommunity.api.insurance.entity.InsuranceEnums.Category;
import com.manacommunity.api.insurance.entity.InsuranceEnums.VerificationStatus;
import com.manacommunity.api.insurance.entity.InsurancePartner;
import com.manacommunity.api.insurance.entity.InsuranceProduct;
import com.manacommunity.api.insurance.entity.InsuranceProductVersion;
import com.manacommunity.api.insurance.repository.InsurancePartnerRepository;
import com.manacommunity.api.insurance.repository.InsuranceProductRepository;
import com.manacommunity.api.insurance.repository.InsuranceProductVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only catalogue. A product is listed only when its partner is ACTIVE with verified authorisation AND
 * the latest published version is VERIFIED (maker-checker approved). No ranking or "best" labelling is applied;
 * ordering is stable by product name so commercial relationships cannot influence position.
 */
@Service
@RequiredArgsConstructor
public class InsuranceCatalogueService {

    private final InsuranceProductRepository products;
    private final InsuranceProductVersionRepository versions;
    private final InsurancePartnerRepository partners;

    public List<CatalogueItem> list(Category category) {
        List<CatalogueItem> out = new ArrayList<>();
        for (InsuranceProduct p : products.findByCategoryAndStatus(category, "ACTIVE")) {
            InsurancePartner partner = partners.findById(p.getPartnerId()).orElse(null);
            if (partner == null || !partner.isSurfaceable()) continue;
            List<InsuranceProductVersion> v = versions
                    .findByProductIdAndVerificationStatusOrderByVersionNoDesc(p.getId(), VerificationStatus.VERIFIED);
            if (v.isEmpty()) continue;
            out.add(toItem(p, v.get(0), partner));
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        return out;
    }

    static CatalogueItem toItem(InsuranceProduct p, InsuranceProductVersion v, InsurancePartner partner) {
        return new CatalogueItem(v.getId(), p.getId(), p.getCategory().name(), p.getSubtype(), p.getName(),
                v.getVersionNo(), partner.getLegalName(), partner.getPartnerType(), partner.getIrdaiRegistrationNo(),
                v.getDataSource(), v.getBrochureUrl(), v.getPolicyWordingUrl(), v.getEligibilityJson(),
                v.getCoverageJson(), v.getBenefitsJson(), v.getWaitingPeriodsJson(), v.getExclusionsJson(),
                v.getNetworkJson(), v.getDisclosuresJson(), partner.getDisclosureText(),
                partner.getCommercialDisclosure(), v.getVerifiedAt());
    }
}
