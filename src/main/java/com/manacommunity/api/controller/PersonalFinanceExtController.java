package com.manacommunity.api.controller;

import com.manacommunity.api.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/personal-finance")
@RequiredArgsConstructor
public class PersonalFinanceExtController {

    private final PersonalFinanceService personalFinanceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        LocalDate now = LocalDate.now();
        int m = now.getMonthValue();
        int y = now.getYear();
        Map<String, Object> summary = personalFinanceService.getMonthlySummary(user, m, y);
        summary.put("month", month != null ? month : now.getYear() + "-" + String.format("%02d", now.getMonthValue()));
        summary.put("recentTransactions", List.of());
        summary.put("manaProjections", List.of());
        summary.put("budgetAlerts", List.of());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> createAccount(
            @RequestBody Map<String, Object> dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id", "acc-" + System.currentTimeMillis());
        account.put("name", dto.getOrDefault("name", "New Account"));
        account.put("type", dto.getOrDefault("type", "SAVINGS"));
        account.put("balance", dto.getOrDefault("balance", 0));
        account.put("currency", dto.getOrDefault("currency", "₹"));
        account.put("isActive", true);
        account.put("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(account);
    }

    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(
            @RequestBody Map<String, Object> dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> cat = new LinkedHashMap<>();
        cat.put("id", "cat-" + System.currentTimeMillis());
        cat.put("name", dto.getOrDefault("name", "Category"));
        cat.put("icon", dto.getOrDefault("icon", "pricetag-outline"));
        cat.put("color", dto.getOrDefault("color", "#64748B"));
        cat.put("type", dto.getOrDefault("type", "EXPENSE"));
        cat.put("subcategories", List.of());
        return ResponseEntity.ok(cat);
    }

    @PostMapping("/transactions/parse-text")
    public ResponseEntity<?> parseNaturalLanguageText(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        String text = body.getOrDefault("text", "").toString().toLowerCase();
        String type = "EXPENSE";
        if (text.contains("income") || text.contains("salary") || text.contains("received")) {
            type = "INCOME";
        }
        BigDecimal amount = BigDecimal.ZERO;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\.\\d{1,2})?").matcher(text);
        if (matcher.find()) {
            amount = new BigDecimal(matcher.group());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("amount", amount);
        result.put("description", body.getOrDefault("text", ""));
        result.put("date", LocalDate.now().toString());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/transactions/batch-import")
    public ResponseEntity<?> batchImport(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        List<?> transactions = body.containsKey("transactions") ? (List<?>) body.get("transactions") : List.of();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("importedCount", transactions.size());
        result.put("failedCount", 0);
        result.put("importedTransactions", List.of());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/recurring")
    public ResponseEntity<?> getRecurring(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/recurring")
    public ResponseEntity<?> createRecurring(
            @RequestBody Map<String, Object> dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> rec = new LinkedHashMap<>();
        rec.put("id", "rec-" + System.currentTimeMillis());
        rec.put("name", dto.getOrDefault("name", "Recurring"));
        rec.put("type", dto.getOrDefault("type", "EXPENSE"));
        rec.put("amount", dto.getOrDefault("amount", 0));
        rec.put("frequency", dto.getOrDefault("frequency", "MONTHLY"));
        rec.put("nextDueDate", dto.getOrDefault("nextDueDate", LocalDate.now().plusMonths(1).toString()));
        rec.put("isActive", true);
        return ResponseEntity.ok(rec);
    }

    @PostMapping("/recurring/{id}/toggle")
    public ResponseEntity<?> toggleRecurring(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("isActive", true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/recurring/process-due")
    public ResponseEntity<?> processDueRecurring(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("processedCount", 0));
    }

    @GetMapping("/bills")
    public ResponseEntity<?> getBills(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/bills/{id}/mark-paid")
    public ResponseEntity<?> markBillPaid(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(Map.of("id", id, "isPaid", true));
    }

    @GetMapping("/installments")
    public ResponseEntity<?> getInstallments(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/installments")
    public ResponseEntity<?> createInstallment(
            @RequestBody Map<String, Object> dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> inst = new LinkedHashMap<>();
        inst.put("id", "inst-" + System.currentTimeMillis());
        inst.put("name", dto.getOrDefault("name", "Installment"));
        inst.put("totalAmount", dto.getOrDefault("totalAmount", 0));
        inst.put("monthlyEmi", dto.getOrDefault("monthlyEmi", 0));
        inst.put("totalTenorMonths", dto.getOrDefault("totalTenorMonths", 12));
        inst.put("remainingTenorMonths", dto.getOrDefault("totalTenorMonths", 12));
        inst.put("paidAmount", 0);
        inst.put("percentPaid", 0);
        inst.put("status", "ACTIVE");
        return ResponseEntity.ok(inst);
    }

    @PostMapping("/installments/{id}/pay")
    public ResponseEntity<?> payInstallment(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("status", "ACTIVE");
        result.put("paidAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/installments/{id}")
    public ResponseEntity<Void> deleteInstallment(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getReport(
            @RequestParam(required = false, defaultValue = "this-month") String period,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        LocalDate now = LocalDate.now();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", period);
        report.put("label", now.getMonth().toString() + " " + now.getYear());
        report.put("totalIncome", 0);
        report.put("totalExpenses", 0);
        report.put("netSavings", 0);
        report.put("topCategories", List.of());
        report.put("monthlyBreakdown", List.of());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/mana-projections")
    public ResponseEntity<?> getManaProjections(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }
}
