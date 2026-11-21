package com.manacommunity.api.fm.ops;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FmOpsDashboardService {

    private final JdbcTemplate jdbc;

    // ── Public API ────────────────────────────────────────────────────────────

    public FmOpsDashboardSnapshot getSnapshot(long communityId) {
        return buildSnapshot(communityId);
    }

    // ── Scheduled cache refresh every 5 minutes ───────────────────────────────

    @Scheduled(fixedRate = 300_000)
    public void refreshAllCommunitySnapshots() {
        try {
            List<Long> ids = jdbc.queryForList(
                "SELECT DISTINCT community_id FROM communities WHERE active = true LIMIT 100",
                Long.class
            );
            for (Long cid : ids) {
                try {
                    buildSnapshot(cid);
                } catch (Exception e) {
                    log.warn("FM ops snapshot failed for community {}: {}", cid, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("FM ops refresh skipped: {}", e.getMessage());
        }
    }

    // ── Internal builders ─────────────────────────────────────────────────────

    private FmOpsDashboardSnapshot buildSnapshot(long communityId) {
        return new FmOpsDashboardSnapshot(
            communityId,
            Instant.now(),
            collectComplaintQueue(communityId),
            collectAmenityUtilization(communityId),
            collectDuesCollection(communityId),
            collectEventPipeline(communityId),
            collectSecurityLog(communityId),
            collectStaffRoster(communityId),
            collectBudgetHealth(communityId)
        );
    }

    private FmOpsDashboardSnapshot.ComplaintQueueMetrics collectComplaintQueue(long communityId) {
        try {
            long totalOpen = queryLong(
                "SELECT COUNT(*) FROM complaints WHERE community_id=? AND status IN ('OPEN','ASSIGNED')",
                communityId
            );
            long overdue = queryLong(
                "SELECT COUNT(*) FROM complaints WHERE community_id=? AND status IN ('OPEN','ASSIGNED') AND created_at < NOW() - INTERVAL '7 days'",
                communityId
            );
            long inProgress = queryLong(
                "SELECT COUNT(*) FROM complaints WHERE community_id=? AND status='IN_PROGRESS'",
                communityId
            );
            long resolvedThisMonth = queryLong(
                "SELECT COUNT(*) FROM complaints WHERE community_id=? AND status='RESOLVED' AND resolved_at >= date_trunc('month', NOW())",
                communityId
            );
            long avgDays = queryLong(
                "SELECT COALESCE(AVG(EXTRACT(DAY FROM resolved_at - created_at)),0) FROM complaints WHERE community_id=? AND status='RESOLVED' AND resolved_at >= NOW() - INTERVAL '90 days'",
                communityId
            );
            List<FmOpsDashboardSnapshot.CategoryCount> byCategory = queryList(
                "SELECT COALESCE(category,'OTHER') AS cat, COUNT(*) AS cnt FROM complaints WHERE community_id=? AND status IN ('OPEN','ASSIGNED','IN_PROGRESS') GROUP BY cat ORDER BY cnt DESC LIMIT 6",
                communityId,
                rs -> new FmOpsDashboardSnapshot.CategoryCount(rs.getString("cat"), rs.getLong("cnt"))
            );
            return new FmOpsDashboardSnapshot.ComplaintQueueMetrics(totalOpen, overdue, inProgress, resolvedThisMonth, avgDays, byCategory);
        } catch (Exception e) {
            log.debug("collectComplaintQueue failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.ComplaintQueueMetrics(0, 0, 0, 0, 0, Collections.emptyList());
        }
    }

    private FmOpsDashboardSnapshot.AmenityUtilizationMetrics collectAmenityUtilization(long communityId) {
        try {
            long totalBookings = queryLong(
                "SELECT COUNT(*) FROM bookings WHERE community_id=? AND status='CONFIRMED' AND booking_date >= date_trunc('month', NOW())",
                communityId
            );
            List<FmOpsDashboardSnapshot.AmenityUsage> byAmenity = queryList(
                """
                SELECT a.name, COUNT(b.id) AS bookings,
                       ROUND(COUNT(b.id) * 100.0 / NULLIF(
                           EXTRACT(DAY FROM date_trunc('month', NOW() + INTERVAL '1 month') - date_trunc('month', NOW())) *
                           GREATEST(1, (a.end_time_slot - a.start_time_slot) / 60), 1), 1) AS pct
                FROM bookings b
                JOIN amenities a ON a.id = b.amenity_id
                WHERE b.community_id = ? AND b.status = 'CONFIRMED' AND b.booking_date >= date_trunc('month', NOW())
                GROUP BY a.id, a.name, a.start_time_slot, a.end_time_slot
                ORDER BY bookings DESC LIMIT 6
                """,
                communityId,
                rs -> new FmOpsDashboardSnapshot.AmenityUsage(
                    rs.getString("name"),
                    rs.getLong("bookings"),
                    rs.getDouble("pct")
                )
            );
            List<FmOpsDashboardSnapshot.HourBucket> peakHours = queryList(
                "SELECT EXTRACT(HOUR FROM created_at)::INT AS hr, COUNT(*) AS cnt FROM bookings WHERE community_id=? AND status='CONFIRMED' AND booking_date >= NOW() - INTERVAL '30 days' GROUP BY hr ORDER BY hr",
                communityId,
                rs -> new FmOpsDashboardSnapshot.HourBucket(rs.getInt("hr"), rs.getLong("cnt"))
            );
            double avgPct = byAmenity.isEmpty() ? 0 : byAmenity.stream().mapToDouble(FmOpsDashboardSnapshot.AmenityUsage::utilizationPct).average().orElse(0);
            return new FmOpsDashboardSnapshot.AmenityUtilizationMetrics(totalBookings, avgPct, byAmenity, peakHours);
        } catch (Exception e) {
            log.debug("collectAmenityUtilization failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.AmenityUtilizationMetrics(0, 0, Collections.emptyList(), Collections.emptyList());
        }
    }

    private FmOpsDashboardSnapshot.DuesCollectionMetrics collectDuesCollection(long communityId) {
        try {
            BigDecimal outstanding = queryDecimal(
                "SELECT COALESCE(SUM(amount),0) FROM maintenance_dues WHERE community_id=? AND status='UNPAID'",
                communityId
            );
            BigDecimal collected = queryDecimal(
                "SELECT COALESCE(SUM(amount_paid),0) FROM maintenance_payments WHERE community_id=? AND payment_date >= date_trunc('month', NOW())",
                communityId
            );
            long overdueResidents = queryLong(
                "SELECT COUNT(DISTINCT resident_id) FROM maintenance_dues WHERE community_id=? AND status='UNPAID' AND due_date < NOW()",
                communityId
            );
            long totalResidents = queryLong(
                "SELECT COUNT(*) FROM residents WHERE community_id=? AND active=true",
                communityId
            );
            BigDecimal monthlyBilled = queryDecimal(
                "SELECT COALESCE(SUM(amount),0) FROM maintenance_dues WHERE community_id=? AND due_date >= date_trunc('month', NOW())",
                communityId
            );
            double rate = monthlyBilled.compareTo(BigDecimal.ZERO) == 0 ? 0
                : collected.multiply(BigDecimal.valueOf(100)).divide(monthlyBilled, 1, RoundingMode.HALF_UP).doubleValue();
            BigDecimal avgDues = totalResidents == 0 ? BigDecimal.ZERO
                : outstanding.divide(BigDecimal.valueOf(totalResidents), 0, RoundingMode.HALF_UP);
            return new FmOpsDashboardSnapshot.DuesCollectionMetrics(outstanding, collected, rate, overdueResidents, totalResidents, avgDues);
        } catch (Exception e) {
            log.debug("collectDuesCollection failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.DuesCollectionMetrics(BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, 0, BigDecimal.ZERO);
        }
    }

    private FmOpsDashboardSnapshot.EventPipelineMetrics collectEventPipeline(long communityId) {
        try {
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            LocalDate weekEnd = today.plusDays(7);
            LocalDate monthEnd = today.plusDays(30);
            long thisWeek = queryLong(
                "SELECT COUNT(*) FROM events WHERE community_id=? AND event_date BETWEEN ? AND ?",
                communityId, today, weekEnd
            );
            long thisMonth = queryLong(
                "SELECT COUNT(*) FROM events WHERE community_id=? AND event_date BETWEEN ? AND ?",
                communityId, today, monthEnd
            );
            long pending = queryLong(
                "SELECT COUNT(*) FROM events WHERE community_id=? AND status='PENDING_APPROVAL'",
                communityId
            );
            long regOpen = queryLong(
                "SELECT COUNT(*) FROM events WHERE community_id=? AND registration_open=true AND event_date > NOW()",
                communityId
            );
            long rsvps = queryLong(
                "SELECT COUNT(*) FROM event_registrations er JOIN events e ON e.id=er.event_id WHERE e.community_id=? AND er.created_at >= date_trunc('month', NOW())",
                communityId
            );
            return new FmOpsDashboardSnapshot.EventPipelineMetrics(thisWeek, thisMonth, pending, regOpen, rsvps);
        } catch (Exception e) {
            log.debug("collectEventPipeline failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.EventPipelineMetrics(0, 0, 0, 0, 0);
        }
    }

    private FmOpsDashboardSnapshot.SecurityLogMetrics collectSecurityLog(long communityId) {
        try {
            long incidents = queryLong(
                "SELECT COUNT(*) FROM audit_journal WHERE community_id=? AND action_type='SECURITY_INCIDENT' AND created_at >= date_trunc('month', NOW())",
                communityId
            );
            long unresolved = queryLong(
                "SELECT COUNT(*) FROM platform_alerts WHERE community_id=? AND resolved_at IS NULL AND severity IN ('HIGH','CRITICAL')",
                communityId
            );
            long gateAccess = queryLong(
                "SELECT COUNT(*) FROM visitor_logs WHERE community_id=? AND entry_time >= CURRENT_DATE",
                communityId
            );
            long suspicious = queryLong(
                "SELECT COUNT(*) FROM visitor_logs WHERE community_id=? AND entry_time >= CURRENT_DATE AND flagged=true",
                communityId
            );
            List<FmOpsDashboardSnapshot.IncidentSummary> recent = queryList(
                "SELECT action_type, details, created_at, 'HIGH' AS sev FROM audit_journal WHERE community_id=? AND action_type='SECURITY_INCIDENT' ORDER BY created_at DESC LIMIT 5",
                communityId,
                rs -> new FmOpsDashboardSnapshot.IncidentSummary(
                    rs.getString("action_type"),
                    rs.getString("details"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getString("sev")
                )
            );
            return new FmOpsDashboardSnapshot.SecurityLogMetrics(incidents, unresolved, gateAccess, suspicious, recent);
        } catch (Exception e) {
            log.debug("collectSecurityLog failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.SecurityLogMetrics(0, 0, 0, 0, Collections.emptyList());
        }
    }

    private FmOpsDashboardSnapshot.StaffRosterMetrics collectStaffRoster(long communityId) {
        try {
            long total = queryLong(
                "SELECT COUNT(*) FROM staff WHERE community_id=? AND active=true",
                communityId
            );
            long present = queryLong(
                "SELECT COUNT(*) FROM staff_attendance WHERE community_id=? AND attendance_date=CURRENT_DATE AND status='PRESENT'",
                communityId
            );
            long absent = total - present;
            List<FmOpsDashboardSnapshot.RoleHeadcount> byRole = queryList(
                """
                SELECT s.role, COUNT(*) AS total,
                       COALESCE(SUM(CASE WHEN a.status='PRESENT' THEN 1 ELSE 0 END),0) AS present_today
                FROM staff s
                LEFT JOIN staff_attendance a ON a.staff_id=s.id AND a.attendance_date=CURRENT_DATE
                WHERE s.community_id=? AND s.active=true
                GROUP BY s.role ORDER BY total DESC LIMIT 8
                """,
                communityId,
                rs -> new FmOpsDashboardSnapshot.RoleHeadcount(rs.getString("role"), rs.getLong("total"), rs.getLong("present_today"))
            );
            return new FmOpsDashboardSnapshot.StaffRosterMetrics(total, present, absent, byRole);
        } catch (Exception e) {
            log.debug("collectStaffRoster failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.StaffRosterMetrics(0, 0, 0, Collections.emptyList());
        }
    }

    private FmOpsDashboardSnapshot.BudgetHealthMetrics collectBudgetHealth(long communityId) {
        try {
            BigDecimal fundBalance = queryDecimal(
                "SELECT COALESCE(SUM(amount),0) FROM community_fund_transactions WHERE community_id=? AND transaction_type='CREDIT'",
                communityId
            ).subtract(queryDecimal(
                "SELECT COALESCE(SUM(amount),0) FROM community_fund_transactions WHERE community_id=? AND transaction_type='DEBIT'",
                communityId
            ));
            BigDecimal actualMonthly = queryDecimal(
                "SELECT COALESCE(SUM(amount),0) FROM community_fund_transactions WHERE community_id=? AND transaction_type='DEBIT' AND transaction_date >= date_trunc('month', NOW())",
                communityId
            );
            BigDecimal budgeted = queryDecimal(
                "SELECT COALESCE(SUM(monthly_budget),0) FROM community_budget_lines WHERE community_id=? AND active=true",
                communityId
            );
            if (budgeted.compareTo(BigDecimal.ZERO) == 0) budgeted = actualMonthly.multiply(BigDecimal.valueOf(1.1));
            double variance = budgeted.compareTo(BigDecimal.ZERO) == 0 ? 0
                : actualMonthly.subtract(budgeted).multiply(BigDecimal.valueOf(100)).divide(budgeted, 1, RoundingMode.HALF_UP).doubleValue();
            double fundHealth = fundBalance.compareTo(BigDecimal.ZERO) <= 0 ? 0
                : Math.min(100, fundBalance.multiply(BigDecimal.valueOf(100)).divide(
                    budgeted.multiply(BigDecimal.valueOf(6)), 1, RoundingMode.HALF_UP).doubleValue());
            BigDecimal runway = actualMonthly.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(99)
                : fundBalance.divide(actualMonthly, 1, RoundingMode.HALF_UP);
            List<FmOpsDashboardSnapshot.ExpenseCategory> topExpenses = queryList(
                """
                SELECT category, SUM(amount) AS total,
                       ROUND(SUM(amount)*100.0/NULLIF((SELECT SUM(amount) FROM community_fund_transactions WHERE community_id=? AND transaction_type='DEBIT' AND transaction_date >= date_trunc('month', NOW())),0),1) AS pct
                FROM community_fund_transactions
                WHERE community_id=? AND transaction_type='DEBIT' AND transaction_date >= date_trunc('month', NOW()) AND category IS NOT NULL
                GROUP BY category ORDER BY total DESC LIMIT 5
                """,
                communityId,
                rs -> new FmOpsDashboardSnapshot.ExpenseCategory(rs.getString("category"), rs.getBigDecimal("total"), rs.getDouble("pct"))
            ) ;
            return new FmOpsDashboardSnapshot.BudgetHealthMetrics(fundBalance, budgeted, actualMonthly, variance, fundHealth, runway, topExpenses);
        } catch (Exception e) {
            log.debug("collectBudgetHealth failed: {}", e.getMessage());
            return new FmOpsDashboardSnapshot.BudgetHealthMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO, Collections.emptyList());
        }
    }

    // ── JDBC helpers ──────────────────────────────────────────────────────────

    private long queryLong(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0L : v;
    }

    private BigDecimal queryDecimal(String sql, Object... args) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v;
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(java.sql.ResultSet rs) throws java.sql.SQLException;
    }

    private <T> List<T> queryList(String sql, long communityId, RowMapper<T> mapper) {
        return jdbc.query(sql, ps -> ps.setLong(1, communityId), (rs, i) -> mapper.map(rs));
    }

    private <T> List<T> queryList(String sql, long communityId, Object extra1, Object extra2, RowMapper<T> mapper) {
        return jdbc.query(sql, ps -> {
            ps.setLong(1, communityId);
            if (extra1 instanceof java.time.LocalDate d) ps.setObject(2, d);
            else ps.setObject(2, extra1);
            if (extra2 instanceof java.time.LocalDate d) ps.setObject(3, d);
            else ps.setObject(3, extra2);
        }, (rs, i) -> mapper.map(rs));
    }
}
