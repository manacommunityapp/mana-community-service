package com.manacommunity.api.ai.tool;

import com.manacommunity.api.ai.config.AgentSecurityContext;
import com.manacommunity.api.ai.config.AgentSecurityContext.UserContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI tools for billing, invoices, and expense queries.
 *
 * <p>Members can view their own invoices and outstanding balance.
 * Admins can view community-wide expenses, invoice summaries, and
 * financial overviews.</p>
 */
@Slf4j
@Component
@Transactional(readOnly = true)
public class BillingQueryTools {

    @PersistenceContext
    private EntityManager em;

    // ── MY INVOICES (any user) ─────────────────────────────────────────

    @Tool(description = "Show the current user's invoices — amount, GST breakdown, due date, "
            + "and payment status. Can filter by status (UNPAID, PAID, OVERDUE, PARTIAL). "
            + "Read-only, no admin needed.")
    public Object getMyInvoices(
            @ToolParam(required = false, description = "Filter: UNPAID, PAID, OVERDUE, PARTIAL, or ALL (default ALL)")
            String status,
            @ToolParam(required = false, description = "Max results (default 15)") Integer limit) {

        UserContext ctx = AgentSecurityContext.get();

        StringBuilder jpql = new StringBuilder(
                "SELECT i.id, i.invoiceNumber, i.taxableAmount, i.cgst, i.sgst, " +
                "i.totalAmount, i.dueDate, i.status, i.paidAt, " +
                "e.title, e.category " +
                "FROM Invoice i LEFT JOIN i.expense e " +
                "WHERE i.resident.id = :uid AND i.community.id = :comId");

        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            jpql.append(" AND i.status = :st");
        }
        jpql.append(" ORDER BY i.generatedAt DESC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("uid", ctx.userId())
                .setParameter("comId", ctx.communityId());
        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("st", status.toUpperCase());
        }
        query.setMaxResults(limit != null ? limit : 15);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("invoice_number", r[1]);
                    m.put("taxable_amount", r[2]);
                    m.put("cgst", r[3]);
                    m.put("sgst", r[4]);
                    m.put("total_amount", r[5]);
                    m.put("due_date", r[6] != null ? r[6].toString() : null);
                    m.put("status", r[7]);
                    m.put("paid_at", r[8] != null ? r[8].toString() : null);
                    m.put("expense_title", r[9]);
                    m.put("expense_category", r[10]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "Get the current user's outstanding balance — total unpaid/overdue amount "
            + "across all invoices. Read-only.")
    public Object getMyOutstandingBalance() {
        UserContext ctx = AgentSecurityContext.get();

        var rows = em.createQuery(
                "SELECT i.status, COUNT(i), SUM(i.totalAmount) FROM Invoice i " +
                "WHERE i.resident.id = :uid AND i.community.id = :comId " +
                "AND i.status IN ('UNPAID', 'OVERDUE', 'PARTIAL') " +
                "GROUP BY i.status", Object[].class)
                .setParameter("uid", ctx.userId())
                .setParameter("comId", ctx.communityId())
                .getResultList();

        BigDecimal totalOutstanding = BigDecimal.ZERO;
        Map<String, Object> breakdown = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String st = (String) r[0];
            long count = ((Number) r[1]).longValue();
            BigDecimal amount = (BigDecimal) r[2];
            breakdown.put(st.toLowerCase(), Map.of("count", count, "amount", amount));
            totalOutstanding = totalOutstanding.add(amount);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total_outstanding", totalOutstanding);
        result.put("breakdown", breakdown);

        // Next due date
        var nextDue = em.createQuery(
                "SELECT MIN(i.dueDate) FROM Invoice i " +
                "WHERE i.resident.id = :uid AND i.status IN ('UNPAID', 'OVERDUE')", Object.class)
                .setParameter("uid", ctx.userId())
                .getResultList();
        if (!nextDue.isEmpty() && nextDue.get(0) != null) {
            result.put("next_due_date", nextDue.get(0).toString());
        }

        return result;
    }

    // ── ADMIN: COMMUNITY-WIDE BILLING ──────────────────────────────────

    @Tool(description = "[ADMIN] View community-wide expenses with amounts, categories, status, "
            + "and approval info. Read-only.")
    public Object listExpenses(
            @ToolParam(required = false, description = "Filter: PENDING, APPROVED, REJECTED, or ALL")
            String status,
            @ToolParam(required = false, description = "Filter by category: EVENT, MAINTENANCE, SPORTS, UTILITIES, etc.")
            String category,
            @ToolParam(required = false, description = "Max results (default 15)") Integer limit) {

        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", "Admin access required to view community expenses.");
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT e.id, e.title, e.amount, e.category, e.description, " +
                "e.status, e.createdAt, u.fullName " +
                "FROM Expense e LEFT JOIN e.createdBy u " +
                "WHERE e.community.id = :comId");

        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            jpql.append(" AND e.status = :st");
        }
        if (category != null) {
            jpql.append(" AND e.category = :cat");
        }
        jpql.append(" ORDER BY e.createdAt DESC");

        var query = em.createQuery(jpql.toString(), Object[].class)
                .setParameter("comId", ctx.communityId());
        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            query.setParameter("st", status.toUpperCase());
        }
        if (category != null) {
            query.setParameter("cat", category.toUpperCase());
        }
        query.setMaxResults(limit != null ? limit : 15);

        return query.getResultList().stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("expense_id", r[0]);
                    m.put("title", r[1]);
                    m.put("amount", r[2]);
                    m.put("category", r[3]);
                    m.put("description", r[4]);
                    m.put("status", r[5]);
                    m.put("created_at", r[6] != null ? r[6].toString() : null);
                    m.put("created_by", r[7]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Tool(description = "[ADMIN] Get a financial overview of the community — total expenses "
            + "by category, invoice collection stats (paid vs unpaid), and overdue amounts.")
    public Object getFinancialOverview() {
        UserContext ctx = AgentSecurityContext.get();
        if (!ctx.isAdmin()) {
            return Map.of("error", "Admin access required.");
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // Expenses by category
        var expByCat = em.createQuery(
                "SELECT e.category, COUNT(e), SUM(e.amount) FROM Expense e " +
                "WHERE e.community.id = :comId AND e.status = 'APPROVED' " +
                "GROUP BY e.category ORDER BY SUM(e.amount) DESC", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        BigDecimal totalExpenses = BigDecimal.ZERO;
        List<Map<String, Object>> expCategories = new ArrayList<>();
        for (Object[] r : expByCat) {
            BigDecimal amt = (BigDecimal) r[2];
            totalExpenses = totalExpenses.add(amt);
            expCategories.add(Map.of("category", r[0], "count", r[1], "total", amt));
        }
        result.put("total_approved_expenses", totalExpenses);
        result.put("expenses_by_category", expCategories);

        // Invoice collection stats
        var invStats = em.createQuery(
                "SELECT i.status, COUNT(i), SUM(i.totalAmount) FROM Invoice i " +
                "WHERE i.community.id = :comId GROUP BY i.status", Object[].class)
                .setParameter("comId", ctx.communityId())
                .getResultList();

        BigDecimal collected = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        Map<String, Map<String, Object>> invoiceBreakdown = new LinkedHashMap<>();
        for (Object[] r : invStats) {
            String st = (String) r[0];
            BigDecimal amt = (BigDecimal) r[2];
            invoiceBreakdown.put(st.toLowerCase(), Map.of("count", r[1], "amount", amt));
            if ("PAID".equalsIgnoreCase(st)) collected = collected.add(amt);
            else outstanding = outstanding.add(amt);
        }
        result.put("total_collected", collected);
        result.put("total_outstanding", outstanding);
        result.put("collection_rate_pct", collected.add(outstanding).compareTo(BigDecimal.ZERO) > 0
                ? collected.multiply(BigDecimal.valueOf(100)).divide(collected.add(outstanding), 1, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO);
        result.put("invoice_breakdown", invoiceBreakdown);

        // Overdue count
        Long overdueCount = em.createQuery(
                "SELECT COUNT(i) FROM Invoice i WHERE i.community.id = :comId " +
                "AND i.status = 'OVERDUE'", Long.class)
                .setParameter("comId", ctx.communityId())
                .getSingleResult();
        result.put("overdue_invoices", overdueCount);

        return result;
    }
}
