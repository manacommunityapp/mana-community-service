# DPDP Act 2023 Compliance Progress

**Branch:** `compliance/dpdp`  
**Audit Date:** 2026-10-13  
**Auditor:** Claude Sonnet 4.6 (automated) + [LEGAL REVIEW REQUIRED: legal counsel review]  
**Privacy Notice Version:** `2026-10-13-v1`  
**Status:** Draft — Pending Legal Review

---

## Summary

This document records all DPDP Act 2023 compliance decisions, what was built, what needs
lawyer review, and open items. The audit covered the mana-community-service (Spring Boot
backend) and mana-community-app (React frontend).

---

## 1. Personal Data Collection Points Identified

| Data Point | Location | Legal Basis | Sensitivity |
|---|---|---|---|
| Full name | `RegisterRequest.java` | Consent (registration) | Normal |
| Email address | `RegisterRequest.java` | Consent / contractual | Normal |
| Phone number (10-digit) | `RegisterRequest.java` | Consent / contractual | Normal |
| Date of birth | `RegisterRequest.java` | Consent | Normal |
| Gender | `RegisterRequest.java` | Consent | Sensitive* |
| Block & flat number | `RegisterRequest.java` | Contractual (residence) | Normal |
| Aadhaar number | `RegisterRequest.java` | **[LEGAL REVIEW REQUIRED — see §9]** | **Highly sensitive** |
| Payment references | Razorpay via `CommercePaymentService.java` | Contractual | Normal |
| Push token (FCM) | `RegisterPushTokenRequest.java` | Consent | Normal |
| Biometric template hash | `BiometricUserEnrollment.java` | Explicit separate consent | **Sensitive** |
| Chat messages | `ChatMessage.java` | Contractual | Normal |
| Activity logs / usage | `UserHubUsage.java`, `AuditLog.java` | Legitimate interest / consent | Normal |

### Third-Party Services Identified
- **Razorpay** — payment processing (PCI-DSS Level 1)
- **Brevo (Sendinblue)** — transactional email (`EmailService`)
- **Firebase FCM** — push notifications
- **AWS (S3 + RDS)** — cloud hosting and media storage
- **Anthropic Claude API** — AI assistant feature (`AiChatAgentService.java`)

### Analytics / Trackers
- Internal usage analytics (`UserHubUsage`, `BookingAnalytics`) — no external tracker found.
- No Google Analytics, Facebook Pixel, or third-party JS trackers found in frontend bundle.
- **ConsentBanner** added to gate analytics consent before any opt-in analytics are enabled.

---

## 2. Privacy Notice (DPDP Act 2023 s.5, s.6)

**Status:** Draft — [LEGAL REVIEW REQUIRED]

### What was built
- Updated `PrivacyPolicyModal.tsx` / `PrivacyContent` to be a full DPDP-compliant notice including:
  - Specific data categories collected (table)
  - Legal basis for each processing (s.4–7)
  - Third-party service names with privacy-policy links
  - Specific retention periods per data category
  - All six DPDP data-principal rights (s.11–14)
  - Grievance Officer contact section
  - Notice versioning (`PRIVACY_NOTICE_VERSION`)
- Updated `PrivacyPage.tsx` (public `/privacy` route) to render the full notice.

### Open Items / Legal Review Required
- [ ] **DPO name and registered address** — placeholder text must be replaced with actual details.
- [ ] **Cross-border transfers** — confirm whether AWS region (likely `ap-south-1`) qualifies under permissible countries under DPDP Rules once finalised.
- [ ] **Sensitive data definitions** — confirm gender is treated as sensitive personal data under DPDP Rules.
- [ ] **Notice adequacy** — legal counsel should review the notice against s.5 of the DPDP Act and applicable DPDP Rules once finalised.
- [ ] **Language** — consider publishing the notice in Hindi and other regional languages (DPDP Rules may require this).

---

## 3. Opt-in Consent Checkboxes (DPDP Act 2023 s.6)

**Status:** Implemented

### What was built
- `ConsentRecord.java` — new entity persisting one immutable row per user per consent purpose. Never deleted (audit trail).
- `ConsentRecordRepository.java` — queries including `findLatestPerPurposeForUser`.
- `PrivacyController.java` updated — `POST /api/privacy/consents` now persists consent records with IP, collection point, notice version.
- `GET /api/privacy/consents` returns live state from DB (not hardcoded).
- `Signup.tsx` — mandatory Privacy Notice acceptance checkbox added (unticked by default) with Terms link.
- `consentToPrivacyNotice` + `consentNoticeVersion` passed from frontend to `RegisterRequest.java`.
- `AuthServiceImpl.java` — validates `consentToPrivacyNotice == true` before creating the user (throws 400 if missing). Saves ESSENTIAL consent record after user creation.

### Consent Purposes Defined
| ID | Purpose | Required |
|---|---|---|
| `ESSENTIAL` | Gate access, emergency, authentication | Yes (cannot revoke) |
| `PUSH_NOTIFICATIONS` | Push / SMS notifications | No |
| `MARKETING` | Community offers & promotions | No |
| `ANALYTICS` | Anonymous in-app telemetry | No |
| `THIRD_PARTY_SHARING` | Sharing contact with home-service vendors | No |
| `BIOMETRIC` | Biometric turnstile enrolment | No (separate explicit) |

### Open Items
- [ ] Biometric consent (`BIOMETRIC`) must be a separate explicit consent collected at the turnstile enrolment screen — not bundled with registration.
- [ ] Consider adding a "consent withdrawal" email notification to users.
- [ ] Backend consent save for marketing/analytics triggered from Settings tab not fully wired (frontend calls `POST /api/privacy/consents`; confirm API contract with `PrivacySettingsTab.tsx`).

---

## 4. Non-Essential Tracker Consent Banner (DPDP Act 2023 s.6)

**Status:** Implemented

### What was built
- `ConsentBanner.tsx` — banner component with Accept All / Customise / Reject options.
  - Unticked by default for analytics and marketing.
  - Shows after 800ms delay.
  - Saves choices to `localStorage` with notice version; re-shows if notice version changes.
  - Mounted globally in `App.tsx`.

### Open Items
- [ ] Wire `ConsentChoices.analytics` to actually gate any analytics calls (no external analytics currently found; wire when added).
- [ ] Wire `ConsentChoices.marketing` to gate marketing notification dispatch.
- [ ] Consider persisting banner choices to backend via `POST /api/privacy/consents` for authenticated users.

---

## 5. Grievance Contact (DPDP Act 2023 s.13)

**Status:** Partially implemented — [LEGAL REVIEW REQUIRED for actual DPO details]

### What was built
- `GET /api/privacy/grievance-contact` endpoint added to `PrivacyController.java`.
- Grievance Officer section added to `PrivacyContent` (Privacy Notice).
- New **Grievance Contact** tab added to `PrivacyHub.tsx` (authenticated Privacy Centre).
- Grievance Officer link added to **Layout sidebar footer** (`Layout.tsx`).
- Email: `dpo@manacommunity.app` (placeholder — must be a real monitored mailbox).

### Open Items
- [ ] **[LEGAL REVIEW REQUIRED]** Replace placeholder DPO name and address with real details.
- [ ] Ensure `dpo@manacommunity.app` mailbox is monitored and has a 30-day SLA.
- [ ] Add grievance contact to the **mobile app footer** (if applicable).
- [ ] Consider a dedicated `/grievance` public page.

---

## 6. Data-Rights Request Form (DPDP Act 2023 s.12–13)

**Status:** Implemented

### What was built
- `DataRightsRequest.java` — new entity covering: ACCESS, CORRECTION, ERASURE, PORTABILITY, GRIEVANCE, NOMINATION.
- `DataRightsRequestRepository.java`.
- `POST /api/privacy/data-rights` and `GET /api/privacy/data-rights` in `PrivacyController.java`.
- Admin endpoints: `GET /api/privacy/admin/data-rights`, `POST /api/privacy/admin/data-rights/{id}/process`.
- `DataRightsForm.tsx` — frontend form in Privacy Hub (new "Exercise My Rights" tab).
- 30-day `dueBy` field auto-set on creation.

### Open Items
- [ ] Add email notification to DPO when a new rights request is submitted.
- [ ] Add email acknowledgement to the data principal (within 72 hours).
- [ ] Hook NOMINATION right to actual nomination flow (legal design required).
- [ ] Admin UI for processing rights requests (beyond the API).

---

## 7. Data Protection Clause in Terms (DPDP Act 2023 s.5–7, s.13)

**Status:** Draft — [LEGAL REVIEW REQUIRED]

### What was built
- `TermsPage.tsx` — full Terms of Use page at `/terms` including:
  - §6: Data Protection clause (consent, legal basis, rights, retention, grievance, cross-border transfers placeholder).
  - §7: Payment processor disclosure (Razorpay).
  - §8: Limitation of liability (placeholder).
  - §10: Governing law (placeholder).
- Route `/terms` added to `routes.tsx`.
- Terms link added to sidebar footer and Signup consent checkbox.

### Open Items
- [ ] **[LEGAL REVIEW REQUIRED]** §6.7 (cross-border transfers), §8 (limitation of liability), §10 (governing law / jurisdiction), §12 (registered address) — all require legal input.
- [ ] Review data-retention figures in §6.8 against actual business/legal requirements.
- [ ] Translate into Hindi and other relevant languages if DPDP Rules require.

---

## 8. Breach Runbook

**Status:** Implemented — [LEGAL REVIEW REQUIRED]

### What was built
- `BREACH_RUNBOOK.md` — in repository root including:
  - Trigger conditions.
  - 72-hour Board notice template.
  - User (data-principal) notice template.
  - Regulatory notification placeholder (Data Protection Board of India).
  - Post-breach review checklist.
  - Breach Incident Log template.

### Open Items
- [ ] **[LEGAL REVIEW REQUIRED]** Confirm 72-hour Board notice and user notice timelines under final DPDP Rules.
- [ ] Confirm Data Protection Board of India portal/address once operational.
- [ ] Run a tabletop breach exercise against this runbook.
- [ ] Integrate runbook trigger into incident-management system (PagerDuty / Jira).

---

## 9. Security Gaps Flagged

| # | Gap | File / Location | Severity | Recommendation |
|---|---|---|---|---|
| 1 | **Aadhaar collection without explicit separate consent** | `RegisterRequest.java:28`, `AuthServiceImpl.java:196` | HIGH | Aadhaar is a sensitive govt. ID. Either remove this field entirely (recommended) or add a separate explicit consent modal, purpose limitation, and verify compliance with Aadhaar Act 2016 / UIDAI regulations. [LEGAL REVIEW REQUIRED] |
| 2 | ~~**No HTTPS enforcement in SecurityConfig**~~ **✓ Fixed** | `SecurityConfig.java` | HIGH | Added `app.security.require-https` property; set to `true` in production `application.properties` to enforce TLS. |
| 3 | **No CAPTCHA / bot detection on registration** | `AuthController.java`, `Signup.tsx` | MEDIUM | Add reCAPTCHA v3 or hCaptcha to registration and login to prevent automated account creation. Do not rely solely on invite codes for bot protection. |
| 4 | **Consent revocation not propagated to processing pipelines** | `PrivacyController.java` | MEDIUM | When MARKETING or ANALYTICS consent is revoked, downstream services (email dispatch, analytics writes) must check the consent state before processing. Currently consent revocation is recorded but not enforced in downstream systems. |
| 5 | **`aadharNumber` field stored even if blank** | `AppUser.java`, `AuthServiceImpl.java:206` | MEDIUM | `govtIdNumber` is set to a masked Aadhaar even if no Aadhaar was provided. Review whether this field is ever populated with real data and scrub if so. |
| 6 | **AI tool `UserCommunicationTools.java` processes personal data** | `UserCommunicationTools.java` | MEDIUM | AI-assisted operations should check consent before using personal data in prompts. Add a consent check for `AI_PROCESSING` purpose before calling Anthropic API with user PII. |
| 7 | ~~**Biometric consent not separately captured at enrolment**~~ **✓ Backend gated** | `BiometricTurnstileController.java` | HIGH | `POST /api/v1/access/turnstiles/enroll` now calls `ConsentGuard.requireConsent(userId, "BIOMETRIC", …)` — returns HTTP 403 `CONSENT_REQUIRED` if consent not granted. **Remaining:** frontend must show an explicit consent UI before calling this endpoint. [LEGAL REVIEW REQUIRED] |
| 8 | **No rate limiting on data-rights endpoint** | `PrivacyController.java` | LOW | Add rate limiting on `POST /api/privacy/data-rights` to prevent abuse. |

---

## What Was Built (Summary)

| Item | Files Changed | Status |
|---|---|---|
| `ConsentRecord` entity + repo | `ConsentRecord.java`, `ConsentRecordRepository.java` | Done |
| `DataRightsRequest` entity + repo | `DataRightsRequest.java`, `DataRightsRequestRepository.java` | Done |
| Consent persistence in `PrivacyController` | `PrivacyController.java` | Done |
| Grievance & data-rights API endpoints | `PrivacyController.java` | Done |
| Registration consent validation | `AuthServiceImpl.java`, `RegisterRequest.java` | Done |
| DPDP-compliant Privacy Notice | `PrivacyPolicyModal.tsx` | Done (legal review required) |
| Non-essential tracker consent banner | `ConsentBanner.tsx`, `App.tsx` | Done |
| Data-rights request form (frontend) | `DataRightsForm.tsx` | Done |
| Terms of Use page with data-protection clause | `TermsPage.tsx`, `routes.tsx` | Done (legal review required) |
| Privacy Hub — Exercise Rights tab | `PrivacyHub.tsx` | Done |
| Privacy Hub — Grievance Contact tab | `PrivacyHub.tsx` | Done |
| Grievance contact in sidebar footer | `Layout.tsx` | Done |
| Terms of Use link in sidebar footer | `Layout.tsx` | Done |
| Consent checkboxes in Signup | `Signup.tsx` | Done |
| Breach Runbook | `BREACH_RUNBOOK.md` | Done (legal review required) |
| `ConsentGuard` reusable component | `ConsentGuard.java` | Done |
| HTTPS enforcement (property-controlled) | `SecurityConfig.java` | Done |
| Biometric consent gate at enrolment | `BiometricTurnstileController.java` | Done |
| DPDP Progress Log | `DPDP_PROGRESS.md` | This file |

---

## What Needs Lawyer Review

1. **DPO / Grievance Officer details** — real name, address, monitored email.
2. **Privacy Notice adequacy** — against DPDP Act 2023 s.5 and final DPDP Rules.
3. **Terms of Use** — §6.7 (cross-border), §8 (liability cap), §10 (jurisdiction).
4. **Aadhaar collection** — whether collecting at registration is permissible; UIDAI/Aadhaar Act implications.
5. **Biometric data** — explicit consent design for turnstile enrolment.
6. **Data retention periods** — validate against applicable law (Companies Act, GST, IT Act).
7. **Breach notification timelines** — confirm under final DPDP Rules.
8. **Cross-border transfer** — confirm AWS `ap-south-1` and Anthropic API under permissible countries list.
9. **Notice language** — whether Hindi / regional-language versions are required.
10. **NOMINATION right** (s.14) — legal design for the nomination flow.

---

## Open Items Checklist

### Code — Remaining
- [ ] Enforce ANALYTICS/MARKETING consent downstream — when those consents are revoked, gate `UserHubUsage` writes and marketing email dispatch (gap #4).
- [ ] Check PUSH_NOTIFICATIONS consent in `UserCommunicationTools.java` before sending push/SMS (gap #6).
- [ ] Add CAPTCHA to registration and login flows (gap #3).
- [ ] Add rate limiting on `POST /api/privacy/data-rights` (gap #8).
- [ ] Remove or scope-limit Aadhaar field after legal review (gap #1/#5).
- [ ] Admin UI for data-rights request processing (beyond API).

### Legal / Ops — Blocked on External Input
- [ ] Replace DPO placeholder name and address with real details (blocks publication).
- [ ] Legal review of Privacy Notice, Terms of Use, and Breach Runbook.
- [ ] Confirm AWS `ap-south-1` and Anthropic API under DPDP permissible countries.
- [ ] Confirm 72 h Board notice timeline under final DPDP Rules.
- [ ] Assess Aadhaar collection risk under Aadhaar Act 2016 / UIDAI regulations.
- [ ] Confirm Data Protection Board of India portal/address once operational.
- [ ] Tabletop breach exercise against `BREACH_RUNBOOK.md`.
- [ ] Test consent banner and verify no non-essential trackers fire before consent.

### Completed ✓
- [x] `ConsentRecord` entity + repository (immutable audit trail)
- [x] `DataRightsRequest` entity + repository (s.12–13 rights)
- [x] Consent persistence in `PrivacyController` (all 6 purposes)
- [x] Registration consent validation (`AuthServiceImpl`, `RegisterRequest`)
- [x] DPDP-compliant Privacy Notice (`PrivacyPolicyModal.tsx`)
- [x] Non-essential tracker consent banner (`ConsentBanner.tsx`)
- [x] Data-rights request form (`DataRightsForm.tsx`)
- [x] Terms of Use page with data-protection clause (`TermsPage.tsx`)
- [x] Privacy Hub — Exercise Rights + Grievance Contact tabs
- [x] Grievance contact + Terms link in sidebar footer (`Layout.tsx`)
- [x] `ConsentGuard` reusable component (`ConsentGuard.java`)
- [x] Property-controlled HTTPS enforcement (`SecurityConfig.java`)
- [x] Biometric enrolment gated behind `BIOMETRIC` consent (`BiometricTurnstileController.java`)
- [x] Frontend biometric consent UI with explicit DPDP s.6 checkbox (`BiometricEnrollModal.tsx`)
- [x] `enroll` service method + `EnrollBiometricRequest` type (`turnstileService.ts`)
- [x] Email acknowledgement to data principal on rights request submission (`PrivacyController.java`)
- [x] Email notification to DPO on rights request submission (`PrivacyController.java`)
- [x] Breach Runbook (`BREACH_RUNBOOK.md`)
