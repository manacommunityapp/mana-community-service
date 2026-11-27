package com.manacommunity.api.delivery.service;

import com.manacommunity.api.delivery.dto.*;

import java.util.List;

public interface ExpressDeliveryPartnerService {
    ExpressEntryResponse registerDispatch(ExpressEntryDispatchRequest req, String partnerKey);
    ExpressBatchManifestResponse registerBatchDispatch(ExpressBatchDispatchRequest req, String partnerKey);
    ExpressVerifyResponse verifyEntry(ExpressVerifyRequest req);
    ExpressBatchManifestResponse verifyBatchManifest(ExpressVerifyRequest req);
    ExpressVerifyResponse checkoutExit(ExpressCheckoutRequest req);
    ExpressBatchManifestResponse reconcileBatchExit(ExpressBatchCheckoutRequest req);
    SocietyGateRulesResponse getSocietyGateRules(Long communityId);
    ResidentDeliveryPreferenceDto getPreferences(Long communityId, String flatNumber);
    ResidentDeliveryPreferenceDto updatePreferences(Long userId, ResidentDeliveryPreferenceDto dto);
    List<ExpressEntryResponse> getActiveDeliveriesForCommunity(Long communityId);
}
