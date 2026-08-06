package com.cpn.application.ai;

import com.cpn.domain.ai.model.AiInsight;
import com.cpn.domain.ai.repository.AiInsightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final AiInsightRepository aiInsightRepository;

    @Transactional
    public AiInsight askAi(UUID userId, String promptCategory, String promptText) {
        String answer = generateResponse(promptCategory, promptText);
        AiInsight insight = AiInsight.builder()
                .userId(userId)
                .promptCategory(promptCategory)
                .promptText(promptText)
                .responseText(answer)
                .build();
        return aiInsightRepository.save(insight);
    }

    private String generateResponse(String category, String prompt) {
        if ("SALARY_BENCHMARK".equalsIgnoreCase(category)) {
            return "Based on 2026 tech compensation benchmarks in Bangalore, Senior Fullstack Engineers (6-8 yrs exp) earn ₹38L - ₹52L base + equity.";
        } else if ("RESUME_REVIEW".equalsIgnoreCase(category)) {
            return "Resume Audit Complete: Strong technical achievements listed. Recommendation: Quantify impact (e.g. 'Reduced latency by 35%') and highlight system design leadership.";
        }
        return "AI Analysis: Your skill profile places you in the top 10% of community candidates for Senior Architecture and Lead Engineer roles.";
    }
}
