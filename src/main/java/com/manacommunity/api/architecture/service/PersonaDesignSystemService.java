package com.manacommunity.api.architecture.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PersonaDesignSystemService {

    public DesignSystemPlan generatePlan() {
        List<Persona> personas = List.of(
                new Persona("GUARD", "Security Guard",
                        List.of("2-tap maximum for any action", "High-contrast (WCAG AAA)", "60px+ touch targets",
                                "Works on ₹8,000 Android phones (2GB RAM)", "Offline-first with sync",
                                "Voice feedback for barrier operations", "Minimal text — icon-driven UI"),
                        List.of("gate_entry_verify", "visitor_approve_deny", "panic_button", "shift_handover"),
                        new UiConstraints(60, 24, "HIGH_CONTRAST", 2, true,
                                "guard-shell: stripped-down WebView with hardware back button")),

                new Persona("RESIDENT", "Resident / Flat Owner",
                        List.of("Clean consumer-grade UX (think Swiggy/Zepto)", "Quick actions on home screen",
                                "Push notifications with deep links", "Smooth animations (60fps)",
                                "Dark mode support", "Localization (Hindi, Kannada, Telugu, Tamil)"),
                        List.of("visitor_preapprove", "maintenance_payment", "amenity_booking", "community_feed",
                                "helpdesk_raise", "delivery_tracking"),
                        new UiConstraints(44, 16, "STANDARD", -1, false,
                                "resident-app: React Native / Flutter with shared design tokens")),

                new Persona("MC_TREASURER", "Management Committee Treasurer",
                        List.of("Dense data tables (Tally-like)", "Keyboard-navigable grids", "Export to Excel/PDF",
                                "Drill-down from summary to transaction", "Audit trail on every entry",
                                "Multi-select bulk operations", "Financial year toggles"),
                        List.of("billing_dashboard", "collection_report", "vendor_payment_approval",
                                "budget_vs_actual", "audit_log", "gst_reconciliation"),
                        new UiConstraints(32, 12, "DENSE", -1, false,
                                "mc-portal: desktop-first React SPA with virtual-scroll tables"))
        );

        List<ArchitectureLayer> layers = List.of(
                new ArchitectureLayer("Design Token Layer",
                        "Shared tokens (colors, spacing, typography scales) consumed by all three shells",
                        List.of("tokens/colors.json — semantic color tokens (--surface-primary, --text-action)",
                                "tokens/spacing.json — 4px base grid, persona multipliers (guard: 2x, resident: 1x, mc: 0.75x)",
                                "tokens/typography.json — type ramp with persona-specific floor sizes",
                                "Build pipeline: tokens → CSS vars + Tailwind config + React Native StyleSheet")),

                new ArchitectureLayer("Component Library (3-tier)",
                        "Primitives → Composed → Persona-specific",
                        List.of("@mana/primitives — Button, Input, Card, Badge (unstyled, headless)",
                                "@mana/composed — DataTable, ApprovalCard, StatusBadge (use primitives + tokens)",
                                "@mana/guard-ui — GuardActionButton (60px, haptic, 2-tap confirm)",
                                "@mana/resident-ui — QuickActionTile, DeliveryCard, BookingSlot",
                                "@mana/mc-ui — LedgerGrid, BudgetChart, AuditRow (virtual-scroll, keyboard nav)")),

                new ArchitectureLayer("API Contract Layer (BFF Pattern)",
                        "Backend-for-Frontend: each persona gets a tailored API surface",
                        List.of("/api/guard/* — minimal payload, binary allow/deny, offline-queue support",
                                "/api/resident/* — rich DTOs with display strings, pagination, real-time events",
                                "/api/mc/* — bulk endpoints, aggregation queries, export streams (CSV/XLSX)",
                                "Shared domain services underneath — BFF is a thin mapping layer",
                                "API versioning per persona (guard v1 stays stable while resident evolves to v3)")),

                new ArchitectureLayer("Feature Flag & Capability Matrix",
                        "Decouple feature rollout per persona",
                        List.of("LaunchDarkly / Flagsmith context: {persona, community, role, device_tier}",
                                "Guard gets features only after 2-week resident bake",
                                "MC features gated by committee_role (Treasurer vs Secretary vs President)",
                                "Device-tier gating: animation-heavy features disabled on <3GB RAM"))
        );

        List<Tradeoff> tradeoffs = List.of(
                new Tradeoff("Shared Monorepo vs Separate Repos",
                        "Monorepo (Nx/Turborepo): shared tokens and primitives, atomic commits across shells",
                        "Coupling risk: guard CI breaks when resident component changes",
                        "Monorepo + per-persona CI pipelines with affected-only builds (nx affected)"),
                new Tradeoff("Single App vs Multiple Apps",
                        "Single APK with role-based shell switching vs separate apps per persona",
                        "Guard app must be <15MB for low-end phones; resident app can be 40MB",
                        "Separate APKs with shared core module (authentication, networking, offline sync)"),
                new Tradeoff("SSR vs SPA for MC Portal",
                        "MC Treasurer needs fast initial load for 10,000-row ledgers",
                        "SPA gives better drill-down UX; SSR gives faster TTFB",
                        "SPA with virtual-scroll + server-side aggregation (don't send 10K rows to client)"),
                new Tradeoff("Design System Governance",
                        "Who approves changes that affect all three personas?",
                        "Design committee slows velocity; no governance leads to divergence",
                        "Tiered: primitives need 2 approvals, persona-specific components need 1")
        );

        return new DesignSystemPlan(personas, layers, tradeoffs);
    }

    public PersonaApiAnalysis analyzeApiComplexity(String persona, int totalEndpoints,
                                                     int sharedEndpoints, int personaSpecific) {
        double couplingRatio = (double) sharedEndpoints / totalEndpoints;
        String couplingLevel = couplingRatio > 0.7 ? "HIGH" : couplingRatio > 0.4 ? "MEDIUM" : "LOW";

        List<String> recommendations = new ArrayList<>();
        if (couplingRatio > 0.5) {
            recommendations.add("Extract shared endpoints into a core BFF module to reduce cross-persona coupling");
        }
        if (persona.equals("GUARD") && totalEndpoints > 10) {
            recommendations.add("Guard API surface too large — consolidate into max 8 endpoints with composite DTOs");
        }
        if (persona.equals("MC_TREASURER") && personaSpecific < 5) {
            recommendations.add("MC portal likely reusing resident endpoints — create dedicated aggregation endpoints");
        }

        int independentDeployScore = (int) ((1 - couplingRatio) * 100);

        return new PersonaApiAnalysis(persona, totalEndpoints, sharedEndpoints, personaSpecific,
                couplingLevel, couplingRatio, independentDeployScore, recommendations);
    }

    public record DesignSystemPlan(List<Persona> personas, List<ArchitectureLayer> layers,
                                    List<Tradeoff> tradeoffs) {}

    public record Persona(String id, String name, List<String> requirements, List<String> keyScreens,
                          UiConstraints constraints) {}

    public record UiConstraints(int minTouchTargetPx, int minFontSizePx, String contrastMode,
                                 int maxTapsPerAction, boolean offlineFirst, String shellDescription) {}

    public record ArchitectureLayer(String name, String description, List<String> details) {}

    public record Tradeoff(String topic, String optionA, String optionB, String recommendation) {}

    public record PersonaApiAnalysis(String persona, int totalEndpoints, int sharedEndpoints,
                                      int personaSpecific, String couplingLevel, double couplingRatio,
                                      int independentDeployScore, List<String> recommendations) {}
}
