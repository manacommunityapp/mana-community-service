# Event Management ERP — Backend Design Spec

**Date:** 2026-08-01
**Status:** Approved
**Scope:** Backend only (Spring Boot APIs). Frontend follows separately.
**Target Events:** Festivals (Ganesh Chaturthi, Navratri, Diwali), New Year celebrations, school/college reunions, community events.

---

## Architecture Decision

- **Approach A — Flat package with sub-packages** inside `com.manacommunity.api.event/`
- Separate entity tree, independent from sports/tournament domain
- Shared infrastructure reused: notifications, email, venues, inventory, food, vendors, finance
- 4 sub-phases, each with its own implementation cycle

## Package Structure

```
com.manacommunity.api.event/
├── controller/
├── service/
├── entity/
├── repository/
├── dto/
└── enums/
```

---

## Sub-Phase 1a: Core (Foundation)

### Entities

#### Event (root)

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| community | Community (FK) | Multi-tenant scoping |
| name | String | "Ganesh Chaturthi 2027" |
| eventType | EventType enum | FESTIVAL, NEW_YEAR, REUNION, CULTURAL, CONFERENCE, CAMP, CELEBRATION, OTHER |
| description | String (TEXT) | Rich description |
| tagline | String | Theme line — "Unity & Devotion" |
| bannerImageUrl | String | Banner/hero image |
| startDate | LocalDate | Event start |
| endDate | LocalDate | Event end |
| totalDays | int | Computed or explicit |
| status | EventStatus enum | DRAFT, PUBLISHED, REGISTRATION_OPEN, LIVE, COMPLETED, CANCELLED |
| visibility | EventVisibility enum | PUBLIC, COMMUNITY_ONLY, INVITE_ONLY |
| maxCapacity | Integer | Optional cap |
| registrationStartDate | LocalDate | When registration opens |
| registrationEndDate | LocalDate | When registration closes |
| budgetAmount | BigDecimal | Planned budget |
| currency | String | "INR" |
| createdBy | AppUser (FK) | Admin who created |
| organizerName | String | "Temple Committee" |
| organizerEmail | String | |
| organizerPhone | String | |
| metadata | String (JSON) | Flexible key-value for event-type-specific data |
| createdAt / updatedAt | LocalDateTime | Audit |

#### EventDay

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | Parent |
| dayNumber | int | 1, 2, 3... |
| date | LocalDate | Actual calendar date |
| title | String | "Day 4 — Lakshmi Pooja" |
| theme | String | Optional day theme |
| description | String | |
| status | DayStatus enum | SCHEDULED, LIVE, COMPLETED, CANCELLED |

#### EventProgram

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| eventDay | EventDay (FK) | Which day |
| event | Event (FK) | Denormalized for querying |
| name | String | "Ganapathi Homam" |
| programType | ProgramType enum | POOJA, CULTURAL, COMPETITION, WORKSHOP, CEREMONY, MEAL, SPEECH, ENTERTAINMENT, MEETING, OTHER |
| startTime | LocalTime | |
| endTime | LocalTime | |
| venue | Venue (FK) | Optional, reuses existing |
| leadTeam | String | "Pooja Team" |
| leadPerson | String | "Pandit Sharma" |
| description | String | |
| maxParticipants | Integer | For competitions/workshops |
| registrationRequired | boolean | Does this program need signup |
| status | ProgramStatus enum | SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED |
| sortOrder | int | Display ordering |

#### EventScheduleItem

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| program | EventProgram (FK) | Parent program |
| title | String | Sub-item within a program |
| startTime | LocalTime | |
| endTime | LocalTime | |
| assignedTo | String | Person/team |
| notes | String | |
| sortOrder | int | |

#### EventCommitteeMember

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| user | AppUser (FK) | Linked user |
| name | String | Display name |
| role | CommitteeRole enum | PRESIDENT, SECRETARY, TREASURER, COORDINATOR, MEMBER, VOLUNTEER_LEAD, FOOD_LEAD, STAGE_LEAD |
| department | String | "Food", "Decorations", "Stage" |
| phone | String | |
| email | String | |
| active | boolean | |

#### EventVenueAssignment

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| venue | Venue (FK) | Reuses existing entity |
| purpose | String | "Main Pandal", "Parking", "Kitchen" |
| isPrimary | boolean | Main event venue |

### APIs

| Method | Endpoint | Purpose | Auth |
|---|---|---|---|
| POST | `/api/events` | Create event | ADMIN, COMMUNITY_ADMIN |
| GET | `/api/events?communityId={id}&type={type}&status={status}` | List (filtered, paged) | Authenticated |
| GET | `/api/events/{id}` | Get detail | Authenticated |
| PUT | `/api/events/{id}` | Update | ADMIN, COMMUNITY_ADMIN |
| PATCH | `/api/events/{id}/status` | Change status | ADMIN |
| DELETE | `/api/events/{id}` | Soft delete | ADMIN |
| GET | `/api/events/{id}/dashboard` | KPI dashboard | ADMIN |
| POST | `/api/events/{eventId}/days` | Create day | ADMIN |
| GET | `/api/events/{eventId}/days` | List days | Authenticated |
| PUT | `/api/events/{eventId}/days/{dayId}` | Update day | ADMIN |
| DELETE | `/api/events/{eventId}/days/{dayId}` | Remove day | ADMIN |
| POST | `/api/events/{eventId}/days/generate` | Auto-generate days from date range | ADMIN |
| POST | `/api/events/{eventId}/programs` | Create program | ADMIN |
| GET | `/api/events/{eventId}/programs?dayId={dayId}&type={type}` | List programs | Authenticated |
| GET | `/api/events/{eventId}/programs/{programId}` | Program detail | Authenticated |
| PUT | `/api/events/{eventId}/programs/{programId}` | Update | ADMIN |
| DELETE | `/api/events/{eventId}/programs/{programId}` | Remove | ADMIN |
| PATCH | `/api/events/{eventId}/programs/{programId}/status` | Update status | ADMIN |
| GET | `/api/events/{eventId}/schedule` | Full schedule (all days + programs) | Authenticated |
| POST | `/api/events/programs/{programId}/items` | Add schedule item | ADMIN |
| PUT | `/api/events/programs/{programId}/items/{itemId}` | Update item | ADMIN |
| DELETE | `/api/events/programs/{programId}/items/{itemId}` | Remove item | ADMIN |
| POST | `/api/events/{eventId}/committee` | Add member | ADMIN |
| GET | `/api/events/{eventId}/committee` | List committee | Authenticated |
| PUT | `/api/events/{eventId}/committee/{memberId}` | Update | ADMIN |
| DELETE | `/api/events/{eventId}/committee/{memberId}` | Remove | ADMIN |
| POST | `/api/events/{eventId}/venues` | Assign venue | ADMIN |
| GET | `/api/events/{eventId}/venues` | List venues | Authenticated |
| PUT | `/api/events/{eventId}/venues/{assignmentId}` | Update | ADMIN |
| DELETE | `/api/events/{eventId}/venues/{assignmentId}` | Remove | ADMIN |

### Services

- **EventService** — CRUD, status state machine, auto-generate days, dashboard aggregation
- **EventProgramService** — program CRUD, time-conflict validation, full schedule view
- **EventCommitteeService** — member management, grouped-by-role listing

### File Count: ~30 files

---

## Sub-Phase 1b: Registrations + Volunteers

### Entities

#### EventRegistration

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| program | EventProgram (FK) | Optional — specific program |
| user | AppUser (FK) | Nullable for walk-in guests |
| registrationType | RegistrationType enum | INDIVIDUAL, FAMILY, VOLUNTEER, VIP, VENDOR, SPONSOR, COMMITTEE, PERFORMER, JUDGE, GUEST |
| name | String | |
| email | String | |
| phone | String | |
| flatNumber | String | For apartment communities |
| numberOfMembers | int | 1 for individual, N for family |
| status | RegistrationStatus enum | PENDING, CONFIRMED, REJECTED, WAITLISTED, CANCELLED, CHECKED_IN |
| approvedBy | AppUser (FK) | |
| approvedAt | LocalDateTime | |
| rejectionReason | String | |
| notes | String | |
| metadata | String (JSON) | Dynamic form data |
| qrCode | String | Generated QR code value |
| checkedIn | boolean | |
| checkedInAt | LocalDateTime | |
| createdAt / updatedAt | LocalDateTime | |

#### EventRegistrationMember

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| registration | EventRegistration (FK) | Parent |
| name | String | |
| age | Integer | |
| gender | String | |
| relation | String | "Spouse", "Child", "Parent" |
| mealPreference | String | "Veg", "Non-Veg", "Jain" |
| checkedIn | boolean | Per-member check-in |

#### EventVolunteer

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| user | AppUser (FK) | |
| registration | EventRegistration (FK) | Linked registration |
| name | String | |
| phone | String | |
| email | String | |
| department | VolunteerDepartment enum | FOOD, DECORATION, STAGE, SECURITY, PARKING, MEDICAL, CLEANING, RECEPTION, POOJA, LOGISTICS, PHOTOGRAPHY |
| status | VolunteerStatus enum | APPLIED, CONFIRMED, ACTIVE, COMPLETED, WITHDRAWN |
| emergencyContactName | String | |
| emergencyContactPhone | String | |
| tshirtSize | String | |
| notes | String | |

#### EventVolunteerShift

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| volunteer | EventVolunteer (FK) | |
| eventDay | EventDay (FK) | |
| shiftStart | LocalTime | |
| shiftEnd | LocalTime | |
| location | String | |
| task | String | |
| attended | boolean | |
| checkedInAt | LocalTime | |
| checkedOutAt | LocalTime | |

#### EventPass

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| registration | EventRegistration (FK) | |
| passType | PassType enum | ENTRY, VIP, VOLUNTEER, PERFORMER, VENDOR, ALL_ACCESS |
| passCode | String (unique) | QR-scannable code |
| holderName | String | |
| validFrom | LocalDate | |
| validTo | LocalDate | |
| scannedCount | int | |
| lastScannedAt | LocalDateTime | |
| active | boolean | |

### APIs

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/events/{eventId}/registrations` | Register |
| GET | `/api/events/{eventId}/registrations?type={}&status={}&programId={}` | List |
| GET | `/api/events/{eventId}/registrations/{regId}` | Detail |
| PUT | `/api/events/{eventId}/registrations/{regId}` | Update |
| PATCH | `/api/events/{eventId}/registrations/{regId}/status` | Approve/reject |
| PATCH | `/api/events/{eventId}/registrations/{regId}/check-in` | Check-in |
| POST | `/api/events/{eventId}/registrations/bulk-approve` | Bulk approve |
| GET | `/api/events/{eventId}/registrations/stats` | Counts |
| POST | `/api/events/registrations/{regId}/members` | Add family member |
| GET | `/api/events/registrations/{regId}/members` | List members |
| PUT | `/api/events/registrations/{regId}/members/{memberId}` | Update |
| DELETE | `/api/events/registrations/{regId}/members/{memberId}` | Remove |
| POST | `/api/events/{eventId}/volunteers` | Register as volunteer |
| GET | `/api/events/{eventId}/volunteers?department={}&status={}` | List |
| PUT | `/api/events/{eventId}/volunteers/{volId}` | Update |
| PATCH | `/api/events/{eventId}/volunteers/{volId}/status` | Confirm/withdraw |
| GET | `/api/events/{eventId}/volunteers/stats` | By department |
| POST | `/api/events/volunteers/{volId}/shifts` | Assign shift |
| GET | `/api/events/{eventId}/shifts?dayId={}&department={}` | List shifts |
| PATCH | `/api/events/volunteers/{volId}/shifts/{shiftId}/attendance` | Mark attendance |
| POST | `/api/events/{eventId}/passes/generate` | Generate passes |
| GET | `/api/events/{eventId}/passes?type={}` | List passes |
| GET | `/api/events/passes/{passCode}/scan` | Scan/validate |
| PATCH | `/api/events/passes/{passCode}/deactivate` | Deactivate |

### Services

- **EventRegistrationService** — all registration types, capacity validation, async email notifications, duplicate detection
- **EventVolunteerService** — volunteer management, department assignments, shift scheduling with conflict detection
- **EventPassService** — UUID pass code generation, scan validation, scan count tracking

### File Count: ~18 files

---

## Sub-Phase 1c: Money (Donations, Sponsors, Auctions, Expenses)

### Entities

#### EventSponsorPackage (community-level, reusable)

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| community | Community (FK) | |
| name | String | "Platinum", "Gold", "Silver", "Bronze" |
| amount | BigDecimal | |
| benefits | String (TEXT) | |
| maxSponsors | Integer | Optional cap per tier |
| sortOrder | int | |
| active | boolean | |

#### EventSponsor

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| sponsorPackage | EventSponsorPackage (FK) | |
| sponsorName | String | |
| contactName / contactEmail / contactPhone | String | |
| flatNumber | String | For resident sponsors |
| user | AppUser (FK) | Optional linked user |
| logoUrl | String | |
| websiteUrl | String | |
| contribution | BigDecimal | Actual amount |
| paymentMode | PaymentMode enum | CASH, UPI, CHEQUE, BANK_TRANSFER, ONLINE |
| paymentReference | String | |
| paymentStatus | PaymentStatus enum | PENDING, RECEIVED, PARTIALLY_RECEIVED |
| sponsorType | SponsorType enum | MONETARY, FOOD, EQUIPMENT, STAGE, DECORATION, VENUE, MEDIA |
| materialDescription | String | For non-monetary |
| acknowledged | boolean | |
| notes | String | |
| createdAt / updatedAt | LocalDateTime | |

#### EventDonation

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| donor | AppUser (FK) | Optional |
| donorName / donorEmail / donorPhone | String | |
| flatNumber | String | |
| donationType | DonationType enum | CASH, UPI, CHEQUE, BANK_TRANSFER, GOLD, SILVER, FOOD, RICE, MILK, VEGETABLES, FLOWERS, EQUIPMENT, MATERIAL, OTHER |
| amount | BigDecimal | For monetary |
| materialDescription | String | For non-monetary |
| materialQuantity | String | |
| estimatedValue | BigDecimal | Estimated value of material |
| paymentReference | String | |
| receiptNumber | String | Auto-generated "DON-2027-0042" |
| receiptGenerated | boolean | |
| anonymous | boolean | |
| purpose | String | "Annadanam", "General Fund" |
| notes | String | |
| status | DonationStatus enum | PLEDGED, RECEIVED, ACKNOWLEDGED |
| receivedBy | String | |
| receivedAt | LocalDateTime | |
| createdAt / updatedAt | LocalDateTime | |

#### EventAuction

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| eventDay | EventDay (FK) | Optional |
| title | String | "Day 4 Evening Auction" |
| auctionType | AuctionType enum | LIVE, SILENT, SEALED |
| status | AuctionStatus enum | DRAFT, OPEN, LIVE, CLOSED, SETTLED |
| startTime / endTime | LocalDateTime | |
| notes | String | |

#### EventAuctionItem

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| auction | EventAuction (FK) | |
| itemName | String | "First Harathi", "Laddu Auction" |
| description | String | |
| category | AuctionItemCategory enum | HARATHI, LADDU, PRASADAM, DECORATION, ANNADANAM, PATTU_VASTRALU, GENERAL |
| imageUrl | String | |
| startingPrice | BigDecimal | |
| reservePrice | BigDecimal | Hidden minimum |
| currentHighestBid | BigDecimal | Denormalized |
| currentHighestBidder | String | Denormalized |
| incrementAmount | BigDecimal | |
| status | AuctionItemStatus enum | UPCOMING, OPEN, SOLD, UNSOLD, WITHDRAWN |
| winnerName / winnerFlatNumber | String | |
| winnerUser | AppUser (FK) | |
| soldPrice | BigDecimal | |
| sortOrder | int | |

#### EventAuctionBid

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| auctionItem | EventAuctionItem (FK) | |
| bidder | AppUser (FK) | Optional |
| bidderName | String | |
| bidderFlatNumber | String | |
| amount | BigDecimal | |
| bidTime | LocalDateTime | |

#### EventAuctionPayment

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| auction | EventAuction (FK) | |
| auctionItem | EventAuctionItem (FK) | |
| payerName | String | |
| amount | BigDecimal | |
| paymentMode | PaymentMode enum | |
| paymentReference | String | |
| status | PaymentStatus enum | |
| receivedBy | String | |
| receivedAt | LocalDateTime | |

#### EventExpense

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| eventDay | EventDay (FK) | Optional |
| category | ExpenseCategory enum | DECORATION, FOOD, STAGE, SOUND, LIGHTING, PRIEST, FLOWERS, TRANSPORT, PRINTING, VENUE, EQUIPMENT, SECURITY, CLEANING, MISC |
| description | String | |
| amount | BigDecimal | |
| vendor | String | |
| paymentMode | PaymentMode enum | |
| paymentReference | String | |
| receiptUrl | String | |
| approvedBy | String | |
| status | ExpenseStatus enum | PENDING, APPROVED, PAID, REJECTED |
| paidBy | String | |
| paidAt | LocalDateTime | |
| createdAt / updatedAt | LocalDateTime | |

### APIs

**Sponsor Packages:** CRUD at `/api/events/sponsor-packages`
**Sponsors:** CRUD + payment recording at `/api/events/{eventId}/sponsors`
**Donations:** CRUD + receipt generation at `/api/events/{eventId}/donations`
**Auctions:** CRUD + items + bids + payments + leaderboard at `/api/events/{eventId}/auctions`
**Expenses:** CRUD + approval at `/api/events/{eventId}/expenses`

### Services

- **EventSponsorService** — package management, per-event sponsors, sponsorship totals
- **EventDonationService** — donation recording, auto-incrementing receipt numbers, totals by type
- **EventAuctionService** — auction lifecycle, bid validation, STOMP WebSocket broadcast to `/topic/event-auction/{auctionId}`, denormalized highest-bid updates
- **EventExpenseService** — expense recording, approval flow, budget-vs-actual

### File Count: ~30 files

---

## Sub-Phase 1d: Operations Integration + Gallery + Reports

### Entities

#### EventFoodPlan

Bridges to existing Food module — does not duplicate it.

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| eventDay | EventDay (FK) | |
| program | EventProgram (FK) | Optional — MEAL program |
| mealType | MealType enum | BREAKFAST, LUNCH, SNACKS, DINNER, PRASADAM, TEA_COFFEE |
| menuDescription | String | |
| expectedHeadcount | int | |
| actualHeadcount | Integer | Post-event |
| cateringMode | CateringMode enum | IN_HOUSE, VENDOR_CATERED, COMMUNITY_CONTRIBUTED |
| vendorName | String | |
| vendorId | Long | FK to existing VmsVendor |
| estimatedCost / actualCost | BigDecimal | |
| notes | String | |
| status | FoodPlanStatus enum | PLANNED, CONFIRMED, IN_PROGRESS, SERVED, CANCELLED |

#### EventInventoryAllocation

References existing `InventoryItem` — does not manage items.

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| eventDay | EventDay (FK) | Optional |
| inventoryItemId | Long | FK to existing InventoryItem |
| itemName | String | Denormalized |
| category | String | "Sound", "Tent", "Lighting" |
| quantityRequired / quantityAllocated / quantityReturned | int | |
| source | AllocationSource enum | OWN, SPONSORED, RENTED, BORROWED |
| vendorName | String | |
| vendorId | Long | FK to existing VmsVendor |
| estimatedCost | BigDecimal | |
| conditionBefore / conditionAfter | String | |
| status | AllocationStatus enum | REQUESTED, ALLOCATED, DISPATCHED, IN_USE, RETURNED, DAMAGED, LOST |
| notes | String | |

#### EventVendorAssignment

Assigns existing VMS vendors to event roles.

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| vendorId | Long | FK to existing VmsVendor |
| vendorName | String | Denormalized |
| serviceType | VendorServiceType enum | CATERING, DECORATION, SOUND, LIGHTING, TENT, STAGE, PHOTOGRAPHY, VIDEOGRAPHY, SECURITY, CLEANING, TRANSPORT, FLOWERS, PRINTING, OTHER |
| description | String | |
| contactName / contactPhone | String | |
| startDate / endDate | LocalDate | |
| agreedAmount / paidAmount | BigDecimal | |
| paymentStatus | PaymentStatus enum | |
| purchaseOrderRef | String | Links to VMS PO |
| status | AssignmentStatus enum | PROPOSED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED |
| rating | Integer | Post-event 1-5 |
| feedback | String | |

#### EventGallery

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| event | Event (FK) | |
| title | String | "Day 1 — Inauguration" |
| description | String | |
| coverImageUrl | String | |
| eventDay | EventDay (FK) | Optional |
| visibility | EventVisibility enum | PUBLIC, COMMUNITY_ONLY, INVITE_ONLY |
| sortOrder | int | |

#### EventGalleryMedia

| Field | Type | Purpose |
|---|---|---|
| id | Long | PK |
| gallery | EventGallery (FK) | |
| mediaType | MediaType enum | PHOTO, VIDEO, HIGHLIGHT |
| url | String | Storage URL |
| thumbnailUrl | String | |
| caption | String | |
| uploadedBy | AppUser (FK) | |
| uploadedByName | String | |
| tags | String | Comma-separated |
| featured | boolean | Show in highlights |
| sortOrder | int | |
| createdAt | LocalDateTime | |

### APIs

**Food Plans:** CRUD + actuals at `/api/events/{eventId}/food-plans`
**Inventory:** CRUD + return at `/api/events/{eventId}/inventory`
**Vendor Assignments:** CRUD + payment + rating at `/api/events/{eventId}/vendors`
**Gallery:** Albums + media CRUD + highlights at `/api/events/{eventId}/galleries`
**Reports:** Read-only aggregation endpoints at `/api/events/{eventId}/reports/{type}`

Report types: `finance`, `registrations`, `food`, `inventory`, `volunteers`, `summary`

### Services

- **EventFoodPlanService** — meal plan CRUD, post-event actuals, stats
- **EventInventoryService** — allocation, return tracking, damage/loss
- **EventVendorAssignmentService** — vendor assignment, payment tracking, post-event rating
- **EventGalleryService** — album management, media upload (base64 initially, S3 later)
- **EventReportService** — read-only aggregation across all sub-tables

### File Count: ~28 files

---

## Exception Handling

### Reused Exceptions (10)

| Exception | Used For |
|---|---|
| ResourceNotFoundException | Event/Day/Program/Registration not found |
| AlreadyRegisteredException | Duplicate registration |
| EventFullException | Event at capacity |
| RegistrationClosedException | Registration window closed |
| InvalidBidAmountException | Bid < current + increment |
| AuctionStateException | Auction in wrong state for operation |
| InsufficientBudgetException | Expense exceeds budget |
| InvalidInputException | Bad dates, invalid enums |
| UnauthorizedActionException | Unauthorized status change |
| InvalidFileUploadException | Invalid gallery upload |

### New Exceptions (3)

| Exception | HTTP | Error Code | Scenario |
|---|---|---|---|
| InvalidEventStateTransitionException | 409 | INVALID_STATE_TRANSITION | e.g., DRAFT → COMPLETED |
| InvalidPassException | 400 | INVALID_PASS | Deactivated/expired pass scanned |
| ShiftConflictException | 409 | SHIFT_CONFLICT | Overlapping volunteer shifts |

All three extend `ManaCommunityException` and are caught by `GlobalExceptionHandler`.

### Event Status State Machine

```
DRAFT → PUBLISHED → REGISTRATION_OPEN → LIVE → COMPLETED
                                              → CANCELLED (from any state except COMPLETED)
```

---

## Async Notification Pattern

Status changes and bulk emails use the `@TransactionalEventListener` + `@Async` pattern (already implemented for tournaments in this codebase):

1. Service publishes `EventStatusChangedEvent` after DB commit
2. Listener fires on background thread
3. Renders and sends emails via existing `EmailService` + `SmtpEmailService`
4. Persists in-app notifications via `NotificationManagementService`
5. HTTP response returns immediately

Live auction bids broadcast via STOMP WebSocket to `/topic/event-auction/{auctionId}`.

---

## Enums Summary

```
EventType: FESTIVAL, NEW_YEAR, REUNION, CULTURAL, CONFERENCE, CAMP, CELEBRATION, OTHER
EventStatus: DRAFT, PUBLISHED, REGISTRATION_OPEN, LIVE, COMPLETED, CANCELLED
EventVisibility: PUBLIC, COMMUNITY_ONLY, INVITE_ONLY
DayStatus: SCHEDULED, LIVE, COMPLETED, CANCELLED
ProgramType: POOJA, CULTURAL, COMPETITION, WORKSHOP, CEREMONY, MEAL, SPEECH, ENTERTAINMENT, MEETING, OTHER
ProgramStatus: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
CommitteeRole: PRESIDENT, SECRETARY, TREASURER, COORDINATOR, MEMBER, VOLUNTEER_LEAD, FOOD_LEAD, STAGE_LEAD
RegistrationType: INDIVIDUAL, FAMILY, VOLUNTEER, VIP, VENDOR, SPONSOR, COMMITTEE, PERFORMER, JUDGE, GUEST
RegistrationStatus: PENDING, CONFIRMED, REJECTED, WAITLISTED, CANCELLED, CHECKED_IN
VolunteerDepartment: FOOD, DECORATION, STAGE, SECURITY, PARKING, MEDICAL, CLEANING, RECEPTION, POOJA, LOGISTICS, PHOTOGRAPHY
VolunteerStatus: APPLIED, CONFIRMED, ACTIVE, COMPLETED, WITHDRAWN
PassType: ENTRY, VIP, VOLUNTEER, PERFORMER, VENDOR, ALL_ACCESS
PaymentMode: CASH, UPI, CHEQUE, BANK_TRANSFER, ONLINE
PaymentStatus: PENDING, RECEIVED, PARTIALLY_RECEIVED
DonationType: CASH, UPI, CHEQUE, BANK_TRANSFER, GOLD, SILVER, FOOD, RICE, MILK, VEGETABLES, FLOWERS, EQUIPMENT, MATERIAL, OTHER
DonationStatus: PLEDGED, RECEIVED, ACKNOWLEDGED
SponsorType: MONETARY, FOOD, EQUIPMENT, STAGE, DECORATION, VENUE, MEDIA
AuctionType: LIVE, SILENT, SEALED
AuctionStatus: DRAFT, OPEN, LIVE, CLOSED, SETTLED
AuctionItemCategory: HARATHI, LADDU, PRASADAM, DECORATION, ANNADANAM, PATTU_VASTRALU, GENERAL
AuctionItemStatus: UPCOMING, OPEN, SOLD, UNSOLD, WITHDRAWN
ExpenseCategory: DECORATION, FOOD, STAGE, SOUND, LIGHTING, PRIEST, FLOWERS, TRANSPORT, PRINTING, VENUE, EQUIPMENT, SECURITY, CLEANING, MISC
ExpenseStatus: PENDING, APPROVED, PAID, REJECTED
MealType: BREAKFAST, LUNCH, SNACKS, DINNER, PRASADAM, TEA_COFFEE
CateringMode: IN_HOUSE, VENDOR_CATERED, COMMUNITY_CONTRIBUTED
FoodPlanStatus: PLANNED, CONFIRMED, IN_PROGRESS, SERVED, CANCELLED
AllocationSource: OWN, SPONSORED, RENTED, BORROWED
AllocationStatus: REQUESTED, ALLOCATED, DISPATCHED, IN_USE, RETURNED, DAMAGED, LOST
VendorServiceType: CATERING, DECORATION, SOUND, LIGHTING, TENT, STAGE, PHOTOGRAPHY, VIDEOGRAPHY, SECURITY, CLEANING, TRANSPORT, FLOWERS, PRINTING, OTHER
AssignmentStatus: PROPOSED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
MediaType: PHOTO, VIDEO, HIGHLIGHT
```

---

## Total Scope

| Sub-Phase | Entities | Files |
|---|---|---|
| 1a: Core | 6 | ~30 |
| 1b: Registrations + Volunteers | 5 | ~18 |
| 1c: Money | 8 | ~30 |
| 1d: Operations + Gallery + Reports | 5 | ~28 |
| **Total** | **24 entities** | **~106 files** |

All inside `com.manacommunity.api.event/` package.
