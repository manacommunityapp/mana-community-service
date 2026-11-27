package com.manacommunity.api.water.anomaly.service;

import com.manacommunity.api.water.anomaly.model.*;

public interface WaterAnomalyDetectionService {

    TankerVerificationResult verifyTankerDelivery(TankerDeliveryVerificationRequest request);

    NightFlowLeakAnalysisResult detectNightFlowLeaks(NightFlowLeakAnalysisRequest request);

    SocietyMassBalanceResult calculateMassBalance(SocietyMassBalanceRequest request);
}
