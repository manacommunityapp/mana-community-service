# Data Breach Response Runbook

> **[LEGAL REVIEW REQUIRED]** This runbook template must be reviewed and approved by
> legal counsel before use. Confirm DPO contact details, Board composition, regulatory
> body name/address, and applicable thresholds under the DPDP Act 2023 and applicable
> DPDP Rules before finalising.

**Version:** 2026-10-13-v1  
**Owner:** Data Protection Officer (DPO)  
**Applicability:** Mana Community Pvt. Ltd. — all personal data processing systems

---

## 1. Trigger Conditions

Activate this runbook when any of the following occur:

| # | Trigger |
|---|---------|
| 1 | Unauthorised access to production database or S3 buckets |
| 2 | Credential / API-key exfiltration confirmed or suspected |
| 3 | Ransomware, malware, or destructive attack on infrastructure |
| 4 | Employee or contractor misuse of personal data |
| 5 | Third-party vendor (Razorpay, Brevo, Firebase, AWS) reports a breach affecting our data |
| 6 | Unexpected bulk data export or anomalous API usage |

---

## 2. Immediate Containment (0–4 hours)

- [ ] **Isolate** affected systems (revoke IAM keys, disable compromised accounts, segment network).
- [ ] **Preserve evidence** — snapshot logs, DB audit tables (`consent_records`, `privacy_audit_log`), access logs. Do not wipe.
- [ ] **Revoke tokens** — rotate all JWT signing keys, OAuth secrets, Razorpay API keys.
- [ ] **Disable inbound data flows** from affected third parties until scope is assessed.
- [ ] **Notify** DPO and on-call Engineering Lead immediately.

**First Responder Contact:**  
`[LEGAL REVIEW REQUIRED: Insert DPO phone / email / pager]`

---

## 3. Assessment (4–24 hours)

- [ ] Determine **categories of personal data** affected (names, emails, phones, flat numbers, payment data, biometric templates, health data).
- [ ] Estimate **number of data principals** impacted.
- [ ] Determine whether data has been exfiltrated or only accessed internally.
- [ ] Assess likelihood of harm: financial fraud, identity theft, physical harm (gate-access logs breach).
- [ ] Document findings in the **Breach Incident Log** (template in § 8).

---

## 4. Board Notification (within 72 hours of discovery)

> **DPDP Act 2023, s.8(6) read with draft DPDP Rules — [LEGAL REVIEW REQUIRED for exact
> threshold and form of notice under final Rules].**

Draft Board Notice template:

---

**CONFIDENTIAL — BOARD NOTICE**

**Date:** [DATE]  
**Subject:** Personal Data Breach — Incident [ID]

Dear Board Members,

We write to inform you that a personal data breach was discovered on [DATE/TIME]. The following is a preliminary summary:

1. **Nature of the breach:** [Unauthorised access / Exfiltration / Insider misuse]  
2. **Data categories affected:** [e.g., name, email, phone, payment reference IDs]  
3. **Estimated number of data principals affected:** [NUMBER or "under assessment"]  
4. **Likely consequences:** [Describe potential harm]  
5. **Measures taken or proposed:**  
   - Containment actions completed: [LIST]  
   - Remediation in progress: [LIST]  
6. **Regulatory notification status:** [Pending / Notified to Data Protection Board on DATE]  

Action required: Approval of user notification and regulatory filing.

Signed,  
[DPO NAME]  
Data Protection Officer, Mana Community Pvt. Ltd.  
[DATE]

---

## 5. Regulatory Notification

> **[LEGAL REVIEW REQUIRED]** Confirm notification obligations, timeline (likely 72 hours
> under draft Rules), prescribed form, and contact details for the Data Protection Board
> of India once the DPDP Rules are finalised.

- [ ] File incident report with **Data Protection Board of India** via prescribed portal/form.
- [ ] Retain acknowledgement receipt.

Regulatory body (draft):  
**Data Protection Board of India**  
`[LEGAL REVIEW REQUIRED: Insert address and portal URL once finalised]`

---

## 6. User (Data Principal) Notification

> Send within **[LEGAL REVIEW REQUIRED: confirm timeline under final DPDP Rules]** of
> confirming a notifiable breach.

**User Notice Template:**

---

**Subject:** Important: Security Notice Regarding Your My Community Account

Dear [NAME / "My Community Member"],

We are writing to inform you about a security incident that may have affected your personal information stored in My Community.

**What happened:**  
[Plain-language description — e.g., "On [DATE], we discovered that unauthorised access occurred to a subset of our systems."]

**What information was involved:**  
[e.g., Name, email address, phone number, flat/block details. No payment card numbers or Aadhaar numbers were stored in the affected system.]

**What we have done:**  
- [Containment action 1]  
- [Containment action 2]  
- Notified the Data Protection Board of India.

**What you should do:**  
- Change your My Community password immediately at [LINK].  
- Monitor your bank and email accounts for suspicious activity.  
- Contact our Grievance Officer (below) if you have questions.

**Grievance Officer Contact:**  
[LEGAL REVIEW REQUIRED: Insert DPO name, email, phone, address]  
Email: dpo@manacommunity.app  
Response time: 30 days

We sincerely apologise for any inconvenience caused.

Mana Community Pvt. Ltd.

---

## 7. Post-Breach Review (within 30 days)

- [ ] Root-cause analysis documented.
- [ ] Security patches applied (patch CVEs, rotate all credentials).
- [ ] Technical controls reviewed: encryption at rest, HTTPS enforcement, WAF rules, rate limits.
- [ ] Data-minimisation review: remove unnecessary personal data fields (e.g., assess `aadharNumber` in `RegisterRequest`).
- [ ] DPDP_PROGRESS.md updated with lessons learned.
- [ ] Board sign-off on remediation.

---

## 8. Breach Incident Log (one entry per event)

| Field | Value |
|---|---|
| Incident ID | |
| Discovery date/time | |
| Reported by | |
| Categories affected | |
| Data principals affected | |
| Containment date | |
| Board notified | |
| Regulatory filing date | |
| User notices sent | |
| RCA completed | |
| Closed | |

---

*[LEGAL REVIEW REQUIRED] This document is a working template. Final version must be
reviewed by qualified legal counsel for compliance with the DPDP Act 2023, applicable
DPDP Rules, IT Act 2000, and any other applicable Indian law.*
