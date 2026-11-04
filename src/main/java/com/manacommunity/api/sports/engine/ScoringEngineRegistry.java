package com.manacommunity.api.sports.engine;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScoringEngineRegistry {

    private final Map<String, ScoringEngine> engines = new ConcurrentHashMap<>();
    private final DefaultScoringEngine defaultEngine;

    public ScoringEngineRegistry(List<ScoringEngine> engineList, DefaultScoringEngine defaultEngine) {
        this.defaultEngine = defaultEngine;
        for (ScoringEngine engine : engineList) {
            engines.put(engine.getSportType().toUpperCase(Locale.ROOT), engine);
        }
    }

    public ScoringEngine getEngine(String sportType) {
        if (sportType == null) return defaultEngine;
        String normalized = sportType.toUpperCase(Locale.ROOT).replace(" ", "_");
        return engines.getOrDefault(normalized, defaultEngine);
    }
}
