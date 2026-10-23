package com.manacommunity.api.insurance;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.*;
import com.manacommunity.api.insurance.entity.*;
import com.manacommunity.api.insurance.entity.InsuranceEnums.*;
import com.manacommunity.api.insurance.partner.*;
import com.manacommunity.api.insurance.repository.*;
import com.manacommunity.api.insurance.service.*;
import com.manacommunity.api.user.model.AppUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Insurance hub unit tests")
class InsuranceHubTest {

    // ---------- webhook signatures
    @Test
    void webhookValidSignatureAccepted() {
        byte[] body = "{\"eventId\":\"e1\"}".getBytes(StandardCharsets.UTF_8);
        long now = 1_700_000_000L;
        String h = WebhookSignatureVerifier.sign("s3cret", now, body);
        assertEquals(WebhookSignatureVerifier.Result.VALID, WebhookSignatureVerifier.verify("s3cret", h, body, now + 10, 300));
    }

    @Test
    void webhookInvalidSignatureRejected() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String h = WebhookSignatureVerifier.sign("other", 1_700_000_000L, body);
        assertEquals(WebhookSignatureVerifier.Result.MISMATCH, WebhookSignatureVerifier.verify("s3cret", h, body, 1_700_000_000L, 300));
        assertEquals(WebhookSignatureVerifier.Result.MALFORMED, WebhookSignatureVerifier.verify("s3cret", "garbage", body, 1, 300));
        assertEquals(WebhookSignatureVerifier.Result.MISSING_SECRET, WebhookSignatureVerifier.verify("", h, body, 1, 300));
    }

    @Test
    void webhookTamperedBodyAndStaleReplayRejected() {
        byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);
        String h = WebhookSignatureVerifier.sign("k", 1000, body);
        assertEquals(WebhookSignatureVerifier.Result.MISMATCH,
                WebhookSignatureVerifier.verify("k", h, "{\"a\":2}".getBytes(StandardCharsets.UTF_8), 1000, 300));
        assertEquals(WebhookSignatureVerifier.Result.STALE, WebhookSignatureVerifier.verify("k", h, body, 5000, 300));
    }

    // ---------- redirects
    @Test
    void redirectValidatorBlocksOpenRedirects() {
        assertTrue(RedirectValidator.isSafeHandoff("https://partner.example.com/buy?x=1", "partner.example.com"));
        assertFalse(RedirectValidator.isSafeHandoff("http://partner.example.com/", "partner.example.com"));
        assertFalse(RedirectValidator.isSafeHandoff("https://evil.com/", "partner.example.com"));
        assertFalse(RedirectValidator.isSafeHandoff("https://partner.example.com@evil.com/", "partner.example.com"));
        assertFalse(RedirectValidator.isSafeHandoff("//evil.com", "partner.example.com"));
        assertFalse(RedirectValidator.isSafeHandoff("javascript:alert(1)", "partner.example.com"));
    }

    // ---------- quote field validation
    @Test
    void quoteValidatorRejectsUnknownAndSensitiveFields() {
        Map<String, Object> f = new HashMap<>(Map.of("pincode", "560001", "memberAges", List.of(30, 28)));
        f.put("aadhaar", "123412341234");
        Map<String, String> errors = QuoteFieldValidator.validate(Category.HEALTH, f, LocalDate.now());
        assertTrue(errors.containsKey("aadhaar"));
    }

    @Test
    void quoteValidatorPerProduct() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        assertTrue(QuoteFieldValidator.validate(Category.HEALTH,
                Map.of("pincode", "560001", "memberAges", List.of(30)), today).isEmpty());
        assertTrue(QuoteFieldValidator.validate(Category.HEALTH, Map.of("pincode", "12", "memberAges", List.of(30)), today).containsKey("pincode"));
        assertTrue(QuoteFieldValidator.validate(Category.MOTOR_CAR,
                Map.of("registrationNumber", "ka 01 ab 1234", "pincode", "560001"), today).isEmpty());
        assertTrue(QuoteFieldValidator.validate(Category.MOTOR_CAR,
                Map.of("registrationNumber", "bad", "pincode", "560001"), today).containsKey("registrationNumber"));
        assertTrue(QuoteFieldValidator.validate(Category.TRAVEL,
                Map.of("destination", "Dubai", "startDate", "2025-12-01", "endDate", "2026-01-05", "travellers", 2), today)
                .containsKey("startDate"));
        assertTrue(QuoteFieldValidator.validate(Category.LIFE_TERM,
                Map.of("age", 17, "annualIncome", 100000, "smoker", false), today).containsKey("age"));
    }

    // ---------- partner executor
    @Test
    void executorRetriesOnlyRetryableWithBackoff() throws Exception {
        List<Long> sleeps = new ArrayList<>();
        PartnerCallExecutor ex = new PartnerCallExecutor(3, 100, 1000, sleeps::add);
        AtomicInteger calls = new AtomicInteger();
        String out = ex.call(() -> {
            if (calls.incrementAndGet() < 3) throw new PartnerException("HTTP_503", "x", true);
            return "ok";
        });
        assertEquals("ok", out);
        assertEquals(List.of(100L, 200L), sleeps);

        AtomicInteger c2 = new AtomicInteger();
        assertThrows(PartnerException.class, () -> ex.call(() -> { c2.incrementAndGet(); throw new PartnerException("BAD", "x", false); }));
        assertEquals(1, c2.get());
    }

    @Test
    void executorTimesOut() {
        PartnerCallExecutor ex = new PartnerCallExecutor(1, 1, 50, ms -> {});
        PartnerException e = assertThrows(PartnerException.class, () -> ex.call(() -> { Thread.sleep(2000); return 1; }));
        assertEquals("TIMEOUT", e.getCode());
    }

    // ---------- audit chain
    @Test
    void auditChainDetectsTampering() {
        LocalDateTime t = LocalDateTime.of(2026, 1, 1, 0, 0);
        String h1 = InsuranceAuditService.computeHash(null, 1L, "A", "P", "1", "d", t);
        String h2 = InsuranceAuditService.computeHash(h1, 1L, "B", "P", "1", "d", t);
        InsuranceAuditEvent e1 = InsuranceAuditEvent.builder().actorUserId(1L).action("A").entityType("P").entityId("1").detail("d").hash(h1).createdAt(t).build();
        InsuranceAuditEvent e2 = InsuranceAuditEvent.builder().actorUserId(1L).action("B").entityType("P").entityId("1").detail("d").prevHash(h1).hash(h2).createdAt(t).build();
        assertTrue(InsuranceAuditService.verifyChain(List.of(e1, e2)));
        e1.setDetail("tampered");
        assertFalse(InsuranceAuditService.verifyChain(List.of(e1, e2)));
    }

    // ---------- consent
    @Test
    void consentWithdrawalDisablesConsent() {
        ConsentRecordRepository repo = mock(ConsentRecordRepository.class);
        InsuranceAuditService audit = mock(InsuranceAuditService.class);
        InsuranceConsentService svc = new InsuranceConsentService(repo, audit);
        ConsentRecord granted = ConsentRecord.builder().id(1L).userId(7L).purpose(ConsentPurpose.QUOTE_SHARING)
                .noticeVersion("v1").eventType(ConsentEvent.GRANTED).build();
        ConsentRecord withdrawn = ConsentRecord.builder().id(2L).userId(7L).purpose(ConsentPurpose.QUOTE_SHARING)
                .noticeVersion("v1").eventType(ConsentEvent.WITHDRAWN).grantedRefId(1L).build();
        when(repo.findById(1L)).thenReturn(Optional.of(granted));
        when(repo.findFirstByUserIdAndPurposeAndPartnerIdIsNullOrderByIdDesc(7L, ConsentPurpose.QUOTE_SHARING))
                .thenReturn(Optional.of(withdrawn));
        ManaCommunityException ex = assertThrows(ManaCommunityException.class,
                () -> svc.requireActive(7L, 1L, ConsentPurpose.QUOTE_SHARING));
        assertEquals("CONSENT_REQUIRED", ex.getErrorCode());
        // another user's consent id is unusable
        assertThrows(ManaCommunityException.class, () -> svc.requireActive(8L, 1L, ConsentPurpose.QUOTE_SHARING));
    }

    // ---------- quote service
    private QuoteService quoteService(List<PartnerAdapter> adapters, QuoteRequestRepository qr, InsuranceQuoteRepository q,
                                      InsurancePartnerRepository partners, InsuranceConsentService consents) {
        return new QuoteService(qr, q, partners, consents, mock(InsuranceAuditService.class), adapters, 1, 1, 1000);
    }

    private static AppUser user(long id) {
        AppUser u = new AppUser();
        u.setId(id);
        return u;
    }

    private static ConsentRecord consent() {
        return ConsentRecord.builder().id(5L).userId(7L).purpose(ConsentPurpose.QUOTE_SHARING).noticeVersion("v1")
                .eventType(ConsentEvent.GRANTED).build();
    }

    @Test
    void quoteWithoutPartnerNeverFabricates() {
        QuoteRequestRepository qr = mock(QuoteRequestRepository.class);
        InsuranceQuoteRepository q = mock(InsuranceQuoteRepository.class);
        InsurancePartnerRepository partners = mock(InsurancePartnerRepository.class);
        InsuranceConsentService consents = mock(InsuranceConsentService.class);
        when(consents.requireActive(7L, 5L, ConsentPurpose.QUOTE_SHARING)).thenReturn(consent());
        when(qr.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(qr.save(any())).thenAnswer(i -> { QuoteRequest r = i.getArgument(0); r.setId(1L); return r; });
        when(q.findByQuoteRequestId(any())).thenReturn(List.of());
        QuoteService svc = quoteService(List.of(), qr, q, partners, consents);
        QuoteRequestView v = svc.submit(user(7), "k1", new QuoteSubmitRequest(Category.HEALTH, "ACME", 5L,
                Map.of("pincode", "560001", "memberAges", List.of(30))));
        assertEquals("PARTNER_UNAVAILABLE", v.status());
        assertTrue(v.quotes().isEmpty());
        verify(q, never()).save(any());
    }

    @Test
    void sandboxQuotesAreLabelled() {
        QuoteRequestRepository qr = mock(QuoteRequestRepository.class);
        InsuranceQuoteRepository q = mock(InsuranceQuoteRepository.class);
        InsurancePartnerRepository partners = mock(InsurancePartnerRepository.class);
        InsuranceConsentService consents = mock(InsuranceConsentService.class);
        when(consents.requireActive(7L, 5L, ConsentPurpose.QUOTE_SHARING)).thenReturn(consent());
        when(qr.findByUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(qr.save(any())).thenAnswer(i -> { QuoteRequest r = i.getArgument(0); r.setId(1L); return r; });
        ArgumentCaptor<InsuranceQuote> cap = ArgumentCaptor.forClass(InsuranceQuote.class);
        when(q.save(cap.capture())).thenAnswer(i -> i.getArgument(0));
        when(q.findByQuoteRequestId(1L)).thenAnswer(i -> List.of(cap.getValue()));
        QuoteService svc = quoteService(List.of(new SandboxPartnerAdapter()), qr, q, partners, consents);
        QuoteRequestView v = svc.submit(user(7), "k2", new QuoteSubmitRequest(Category.TRAVEL, "SANDBOX", 5L,
                Map.of("destination", "Goa", "startDate", LocalDate.now().plusDays(3).toString(),
                        "endDate", LocalDate.now().plusDays(6).toString(), "travellers", 2)));
        assertEquals(QuoteSource.SANDBOX, cap.getValue().getDataSource());
        assertEquals("QUOTED", v.status());
        assertTrue(v.quotes().get(0).label().contains("SAMPLE"));
    }

    @Test
    void duplicateSubmissionIsIdempotentAndKeyReuseConflicts() {
        QuoteRequestRepository qr = mock(QuoteRequestRepository.class);
        InsuranceQuoteRepository q = mock(InsuranceQuoteRepository.class);
        InsurancePartnerRepository partners = mock(InsurancePartnerRepository.class);
        InsuranceConsentService consents = mock(InsuranceConsentService.class);
        when(consents.requireActive(any(), any(), any())).thenReturn(consent());
        List<QuoteRequest> store = new ArrayList<>();
        when(qr.findByUserIdAndIdempotencyKey(any(), any())).thenAnswer(i -> store.stream().findFirst());
        when(qr.save(any())).thenAnswer(i -> {
            QuoteRequest r = i.getArgument(0);
            if (r.getId() == null) { r.setId(1L); store.add(r); }
            return r;
        });
        when(q.findByQuoteRequestId(any())).thenReturn(List.of());
        QuoteService svc = quoteService(List.of(), qr, q, partners, consents);
        QuoteSubmitRequest req = new QuoteSubmitRequest(Category.HEALTH, "ACME", 5L, Map.of("pincode", "560001", "memberAges", List.of(30)));
        svc.submit(user(7), "dup", req);
        QuoteRequestView second = svc.submit(user(7), "dup", req);
        assertTrue(second.replayed());
        assertEquals(1, store.size());
        QuoteSubmitRequest other = new QuoteSubmitRequest(Category.HEALTH, "ACME", 5L, Map.of("pincode", "560002", "memberAges", List.of(30)));
        assertEquals("IDEMPOTENCY_KEY_REUSED",
                assertThrows(ManaCommunityException.class, () -> svc.submit(user(7), "dup", other)).getErrorCode());
    }

    @Test
    void quoteRequiresIdempotencyKey() {
        QuoteService svc = quoteService(List.of(), mock(QuoteRequestRepository.class), mock(InsuranceQuoteRepository.class),
                mock(InsurancePartnerRepository.class), mock(InsuranceConsentService.class));
        assertThrows(ManaCommunityException.class, () -> svc.submit(user(7), null,
                new QuoteSubmitRequest(Category.HEALTH, "A", 1L, Map.of())));
    }

    @Test
    void expiredQuoteIsFlagged() {
        InsuranceQuote q = InsuranceQuote.builder().validUntil(LocalDateTime.now().minusMinutes(1)).build();
        assertTrue(q.isExpired(LocalDateTime.now()));
        q.setValidUntil(LocalDateTime.now().plusMinutes(5));
        assertFalse(q.isExpired(LocalDateTime.now()));
    }

    // ---------- policies
    @Test
    void policyOwnerScopingAndMasking() {
        InsurancePolicyRepository repo = mock(InsurancePolicyRepository.class);
        InsurancePolicyService svc = new InsurancePolicyService(repo, mock(PolicyStatusHistoryRepository.class),
                mock(InsuranceConsentService.class), mock(InsuranceAuditService.class),
                mock(com.manacommunity.api.service.FieldEncryptionService.class));
        when(repo.findByIdAndUserId(10L, 8L)).thenReturn(Optional.empty());
        assertEquals("NOT_FOUND", assertThrows(ManaCommunityException.class, () -> svc.get(8L, 10L)).getErrorCode());
        assertEquals("XXXXXX1234", InsurancePolicyService.mask("1234"));
    }

    @Test
    void declaredPolicyIsNeverPresentedAsVerifiedAndDerivesExpiryFromDateOnly() {
        InsurancePolicy p = InsurancePolicy.builder().id(1L).userId(1L).insurerName("X").productName("Y")
                .category(Category.HEALTH).dataSource(PolicySource.USER_DECLARED).status(PolicyStatus.ACTIVE_DECLARED)
                .expiryDate(LocalDate.now().minusDays(1)).build();
        // call through the service's static view via list()
        InsurancePolicyRepository repo = mock(InsurancePolicyRepository.class);
        when(repo.findByUserIdOrderByExpiryDateAsc(1L)).thenReturn(List.of(p));
        InsurancePolicyService svc = new InsurancePolicyService(repo, null, null, null, null);
        PolicyView v = svc.list(1L).get(0);
        assertFalse(v.coverageConfirmedByInsurer());
        assertEquals("EXPIRED", v.status());
        assertTrue(v.statusNote().contains("not verified"));
    }

    @Test
    void partnerStatusSyncIsIdempotent() {
        InsurancePolicyRepository repo = mock(InsurancePolicyRepository.class);
        PolicyStatusHistoryRepository hist = mock(PolicyStatusHistoryRepository.class);
        InsurancePolicyService svc = new InsurancePolicyService(repo, hist, null, mock(InsuranceAuditService.class), null);
        InsurancePolicy p = InsurancePolicy.builder().id(1L).userId(1L).partnerId(9L).insurerName("X").productName("Y")
                .category(Category.HEALTH).dataSource(PolicySource.USER_DECLARED).status(PolicyStatus.ACTIVE_DECLARED).build();
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(hist.existsByPolicyIdAndEventId(1L, "e1")).thenReturn(false, true);
        assertTrue(svc.applyPartnerStatus(1L, 9L, "e1", PolicyStatus.ACTIVE_VERIFIED));
        assertFalse(svc.applyPartnerStatus(1L, 9L, "e1", PolicyStatus.ACTIVE_VERIFIED));
        verify(hist, times(1)).save(any());
        // wrong partner cannot touch another partner's policy
        assertFalse(svc.applyPartnerStatus(1L, 99L, "e2", PolicyStatus.CANCELLED));
    }

    // ---------- claims
    @Test
    void claimsAssistanceNeverCarriesInsurerDecision() {
        ClaimsAssistanceCase c = ClaimsAssistanceCase.builder().id(1L).caseRef("MAC-X").userId(1L).claimType("HEALTH").build();
        ClaimCaseView v = ClaimsAssistanceService.view(c);
        assertNull(v.insurerClaimStatus());
        assertTrue(v.notice().contains("not a claim filed with your insurer"));
        assertFalse(java.util.Arrays.stream(CaseStatus.values()).anyMatch(s -> s.name().contains("APPROV") || s.name().contains("SETTLE")));
    }

    // ---------- admin maker-checker
    @Test
    void makerCannotApproveOwnPartner() {
        InsurancePartnerRepository partners = mock(InsurancePartnerRepository.class);
        InsuranceAdminService svc = new InsuranceAdminService(partners, mock(InsuranceProductRepository.class),
                mock(InsuranceProductVersionRepository.class), mock(PartnerWebhookEventRepository.class),
                mock(InsuranceAuditService.class));
        AppUser admin = user(1);
        admin.setRole("ADMIN");
        when(partners.findById(5L)).thenReturn(Optional.of(InsurancePartner.builder().id(5L).code("A").legalName("A")
                .partnerType("INSURER").irdaiRegistrationNo("123").createdBy(1L).build()));
        assertEquals("MAKER_CHECKER", assertThrows(ManaCommunityException.class,
                () -> svc.approvePartner(admin, 5L, new PartnerApproveRequest("evidence"))).getErrorCode());
    }

    @Test
    void nonAdminCannotUseAdminOperations() {
        InsuranceAdminService svc = new InsuranceAdminService(null, null, null, null, null);
        AppUser u = user(2);
        u.setRole("MEMBER");
        assertEquals("FORBIDDEN", assertThrows(ManaCommunityException.class, () -> svc.listPartners(u)).getErrorCode());
        assertEquals("FORBIDDEN", assertThrows(ManaCommunityException.class, () -> svc.failedWebhooks(u)).getErrorCode());
    }

    @Test
    void unverifiedPartnerNotSurfaceable() {
        InsurancePartner p = InsurancePartner.builder().status(PartnerStatus.ACTIVE).authorisationVerified(false).build();
        assertFalse(p.isSurfaceable());
        p.setAuthorisationVerified(true);
        assertTrue(p.isSurfaceable());
        p.setStatus(PartnerStatus.SUSPENDED);
        assertFalse(p.isSurfaceable());
    }

    @Test
    void catalogueHidesUnverifiedProducts() {
        InsuranceProductRepository prods = mock(InsuranceProductRepository.class);
        InsuranceProductVersionRepository vers = mock(InsuranceProductVersionRepository.class);
        InsurancePartnerRepository partners = mock(InsurancePartnerRepository.class);
        InsuranceCatalogueService svc = new InsuranceCatalogueService(prods, vers, partners);
        when(prods.findByCategoryAndStatus(Category.HEALTH, "ACTIVE")).thenReturn(List.of(
                InsuranceProduct.builder().id(1L).partnerId(1L).category(Category.HEALTH).name("A").build()));
        when(partners.findById(1L)).thenReturn(Optional.of(InsurancePartner.builder().id(1L).status(PartnerStatus.ACTIVE)
                .authorisationVerified(true).legalName("L").partnerType("INSURER").build()));
        when(vers.findByProductIdAndVerificationStatusOrderByVersionNoDesc(1L, VerificationStatus.VERIFIED)).thenReturn(List.of());
        assertTrue(svc.list(Category.HEALTH).isEmpty());
    }
}
