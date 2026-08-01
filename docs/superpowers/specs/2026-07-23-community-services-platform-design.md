# Community Services Platform — Phase 1: Core Service Engine

**Date:** 2026-07-23
**Status:** Draft
**Scope:** Phase 1 of 8 — Service Catalog, Provider Registry, Service Requests, Work Orders

---

## 1. Objective

Build a config-driven Community Services Platform (CSP) engine on the existing mana-community-service backend. The engine provides a single architecture for every service offered within a residential community — from plumbing to yoga coaching to pet grooming — without code changes when new service categories are added.

Phase 1 delivers the foundational entities and APIs: Service Catalog (domains + categories), Provider Registry, Service Requests, and Work Orders. Subsequent phases add scheduling, pricing, AMC/warranty, reviews, AI, analytics, and material tracking.

## 2. Platform Phasing

| Phase | Sub-project | Depends on |
|-------|------------|------------|
| **1** | Core Service Engine (this spec) | — |
| **2** | Scheduling & Availability Engine | Phase 1 |
| **3** | Pricing & Payment Engine | Phase 1 |
| **4** | AMC & Warranty Engine | Phase 1, 3 |
| **5** | Review & Rating Engine | Phase 1 |
| **6** | AI & Recommendation Engine | Phase 1, 5 |
| **7** | Analytics & Reporting Engine | Phase 1 |
| **8** | Material & Equipment Engine | Phase 1 |

Each phase gets its own spec → plan → implementation cycle.

## 3. Architecture Decision

**Approach: Config-Driven Engine Package** within the existing Spring Boot monolith.

- New `com.manacommunity.api.serviceplatform` package following the same layered patterns as `booking` and `marketplace`.
- Reuses existing modules: Vendor, Invoice/Billing, Notification, User/Security, Community.
- JPA entities with PostgreSQL, JSON columns for extensible custom fields.
- Standard REST APIs secured by existing JWT + role-based auth.

Rejected alternatives:
- **Microservice-per-engine:** Premature; adds operational overhead without matching traffic to justify it.
- **CQRS + Event-Sourced:** Complexity mismatch with the existing JPA/CRUD codebase.

## 4. Data Model

### 4.1 ServiceDomain

Represents a top-level service grouping (e.g., "Home Services", "Fitness & Wellness").

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| community_id | UUID | FK → Community; nullable for platform-wide domains |
| name | VARCHAR(100) | e.g., "Home Services" |
| slug | VARCHAR(100) | URL-safe, unique per community scope |
| icon | VARCHAR(50) | emoji or icon key |
| description | TEXT | |
| display_order | INT | sorting weight |
| active | BOOLEAN | soft toggle |
| metadata | JSONB | extensible key-value |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### 4.2 ServiceCategory

Represents a specific service type within a domain. Supports hierarchy via self-referencing parent.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| domain_id | UUID | FK → ServiceDomain |
| parent_category_id | UUID | FK → ServiceCategory (nullable, for sub-categories) |
| name | VARCHAR(100) | e.g., "Electrician" |
| slug | VARCHAR(100) | unique within domain |
| icon | VARCHAR(50) | |
| description | TEXT | |
| required_certifications | JSONB | array of certification names expected for this category |
| custom_fields | JSONB | field definitions for offerings/requests in this category |
| display_order | INT | |
| active | BOOLEAN | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**custom_fields schema:**
```json
[
  {
    "key": "roomCount",
    "label": "Number of Rooms",
    "type": "NUMBER",
    "required": true,
    "options": null
  },
  {
    "key": "cleaningType",
    "label": "Cleaning Type",
    "type": "SELECT",
    "required": true,
    "options": ["Deep Clean", "Regular", "Move-in/Move-out"]
  }
]
```

Supported field types: `TEXT`, `NUMBER`, `SELECT`, `MULTI_SELECT`, `BOOLEAN`, `DATE`, `FILE`.

### 4.3 ServiceProvider

A person or company that provides services. Links to existing `User` (always) and `Vendor` (when the provider is a registered vendor/company).

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| user_id | UUID | FK → User |
| vendor_id | UUID | FK → Vendor (nullable) |
| community_id | UUID | FK → Community |
| provider_type | ENUM | INDIVIDUAL, COMPANY |
| business_name | VARCHAR(200) | |
| phone | VARCHAR(20) | |
| email | VARCHAR(100) | |
| bio | TEXT | |
| profile_image_url | VARCHAR(500) | |
| verification_status | ENUM | PENDING, VERIFIED, REJECTED, SUSPENDED |
| avg_rating | DECIMAL(3,2) | denormalized, updated by review engine |
| total_jobs_completed | INT | denormalized counter |
| service_areas | JSONB | array of area descriptors or geofences |
| certifications | JSONB | array of { name, issuedBy, validUntil, documentUrl } |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### 4.4 ProviderServiceOffering

What a specific provider offers within a specific category.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| provider_id | UUID | FK → ServiceProvider |
| category_id | UUID | FK → ServiceCategory |
| title | VARCHAR(200) | e.g., "Full House Deep Cleaning" |
| description | TEXT | |
| base_price | DECIMAL(12,2) | |
| pricing_unit | ENUM | FLAT, HOURLY, PER_UNIT, CUSTOM |
| estimated_duration_minutes | INT | |
| min_order_value | DECIMAL(12,2) | nullable |
| is_available | BOOLEAN | provider can toggle |
| custom_field_values | JSONB | values matching category's custom_fields schema |
| tags | JSONB | searchable tags array |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### 4.5 ServiceRequest

A resident's request for a service.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| requester_id | UUID | FK → User |
| community_id | UUID | FK → Community |
| category_id | UUID | FK → ServiceCategory |
| title | VARCHAR(200) | |
| description | TEXT | |
| preferred_date | DATE | |
| preferred_time_slot | VARCHAR(50) | e.g., "09:00-12:00" |
| address | TEXT | service location |
| urgency | ENUM | NORMAL, URGENT, EMERGENCY |
| status | ENUM | DRAFT, SUBMITTED, MATCHING, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED, DISPUTED |
| assigned_provider_id | UUID | FK → ServiceProvider (nullable) |
| assigned_offering_id | UUID | FK → ProviderServiceOffering (nullable) |
| estimated_cost | DECIMAL(12,2) | |
| actual_cost | DECIMAL(12,2) | |
| custom_field_values | JSONB | values matching category's custom_fields |
| attachments | JSONB | array of file URLs |
| cancellation_reason | TEXT | |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Status transitions:**
```
DRAFT → SUBMITTED → MATCHING → ASSIGNED → IN_PROGRESS → COMPLETED
                                    ↓                        ↓
                                CANCELLED                DISPUTED
```

- DRAFT: resident saving incomplete request
- SUBMITTED: ready for provider matching
- MATCHING: system/admin looking for providers
- ASSIGNED: provider accepted
- IN_PROGRESS: work order active
- COMPLETED: work done, payment settled
- CANCELLED: by resident or admin (with reason)
- DISPUTED: resident raised a dispute after completion

### 4.6 WorkOrder

Tracks the physical execution of a service request.

| Column | Type | Notes |
|--------|------|-------|
| id | UUID | PK |
| service_request_id | UUID | FK → ServiceRequest (unique) |
| provider_id | UUID | FK → ServiceProvider |
| community_id | UUID | FK → Community |
| scheduled_start | TIMESTAMP | |
| scheduled_end | TIMESTAMP | |
| actual_start | TIMESTAMP | |
| actual_end | TIMESTAMP | |
| status | ENUM | CREATED, SCHEDULED, EN_ROUTE, ARRIVED, IN_PROGRESS, PAUSED, COMPLETED, CANCELLED |
| notes | TEXT | provider notes |
| checklist_items | JSONB | array of { item, completed, completedAt } |
| materials_used | JSONB | array of { name, quantity, unitCost } |
| before_photos | JSONB | array of URLs |
| after_photos | JSONB | array of URLs |
| resident_signoff | BOOLEAN | |
| resident_signoff_at | TIMESTAMP | |
| provider_signoff | BOOLEAN | |
| provider_signoff_at | TIMESTAMP | |
| invoice_id | UUID | FK → Invoice (nullable, created on completion) |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**Status transitions:**
```
CREATED → SCHEDULED → EN_ROUTE → ARRIVED → IN_PROGRESS → COMPLETED
                                                ↓
                                             PAUSED → IN_PROGRESS
     Any status → CANCELLED
```

### 4.7 Entity Relationship Summary

```
Community ──┬── ServiceDomain ── ServiceCategory (self-referencing hierarchy)
            │                          │
            │                          ├── ProviderServiceOffering
            │                          │          │
            │                          │     ServiceProvider ── User
            │                          │          │             Vendor (optional)
            │                          │          │
            │                          └── ServiceRequest ── WorkOrder ── Invoice
            │                                    │
            └────────────────────────────── User (requester)
```

## 5. Package Structure

```
com.manacommunity.api.serviceplatform/
  ├── controller/
  │   ├── ServiceDomainController.java
  │   ├── ServiceCategoryController.java
  │   ├── ServiceProviderController.java
  │   ├── ServiceOfferingController.java
  │   ├── ServiceRequestController.java
  │   └── WorkOrderController.java
  ├── entity/
  │   ├── ServiceDomain.java
  │   ├── ServiceCategory.java
  │   ├── ServiceProvider.java
  │   ├── ProviderServiceOffering.java
  │   ├── ServiceRequest.java
  │   ├── WorkOrder.java
  │   └── enums/
  │       ├── ProviderType.java
  │       ├── VerificationStatus.java
  │       ├── PricingUnit.java
  │       ├── ServiceUrgency.java
  │       ├── ServiceRequestStatus.java
  │       └── WorkOrderStatus.java
  ├── dto/
  │   ├── request/
  │   │   ├── CreateServiceDomainRequest.java
  │   │   ├── CreateServiceCategoryRequest.java
  │   │   ├── RegisterProviderRequest.java
  │   │   ├── CreateOfferingRequest.java
  │   │   ├── CreateServiceRequestDto.java
  │   │   └── UpdateWorkOrderStatusRequest.java
  │   └── response/
  │       ├── ServiceDomainResponse.java
  │       ├── ServiceCategoryResponse.java
  │       ├── ServiceProviderResponse.java
  │       ├── ServiceOfferingResponse.java
  │       ├── ServiceRequestResponse.java
  │       ├── WorkOrderResponse.java
  │       └── ServiceSearchResult.java
  ├── repository/
  │   ├── ServiceDomainRepository.java
  │   ├── ServiceCategoryRepository.java
  │   ├── ServiceProviderRepository.java
  │   ├── ProviderServiceOfferingRepository.java
  │   ├── ServiceRequestRepository.java
  │   └── WorkOrderRepository.java
  ├── service/
  │   ├── ServiceCatalogService.java
  │   ├── ServiceProviderService.java
  │   ├── ServiceRequestService.java
  │   ├── WorkOrderService.java
  │   └── ServiceSearchService.java
  └── exception/
      └── ServicePlatformException.java
```

## 6. API Specification

### 6.1 Admin APIs (ADMIN, SUPER_ADMIN roles)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/service-platform/domains` | Create a service domain |
| PUT | `/api/service-platform/domains/{id}` | Update a domain |
| DELETE | `/api/service-platform/domains/{id}` | Soft-delete a domain |
| POST | `/api/service-platform/categories` | Create a category under a domain |
| PUT | `/api/service-platform/categories/{id}` | Update a category |
| DELETE | `/api/service-platform/categories/{id}` | Soft-delete a category |
| GET | `/api/service-platform/admin/providers` | List providers with filters (status, community) |
| PATCH | `/api/service-platform/admin/providers/{id}/verify` | Approve/reject a provider |
| GET | `/api/service-platform/admin/requests` | List all service requests with filters |
| GET | `/api/service-platform/admin/work-orders` | List all work orders with filters |
| PATCH | `/api/service-platform/admin/requests/{id}/assign` | Manually assign provider to request |

### 6.2 Provider APIs (SERVICE_PROVIDER role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/service-platform/providers/register` | Register as a service provider |
| GET | `/api/service-platform/providers/me` | Get own profile |
| PUT | `/api/service-platform/providers/me` | Update own profile |
| POST | `/api/service-platform/providers/me/offerings` | Create a service offering |
| PUT | `/api/service-platform/providers/me/offerings/{id}` | Update an offering |
| DELETE | `/api/service-platform/providers/me/offerings/{id}` | Remove an offering |
| GET | `/api/service-platform/providers/me/offerings` | List own offerings |
| GET | `/api/service-platform/providers/me/requests` | Incoming service requests |
| PATCH | `/api/service-platform/providers/me/requests/{id}/accept` | Accept a request |
| PATCH | `/api/service-platform/providers/me/requests/{id}/decline` | Decline a request |
| GET | `/api/service-platform/providers/me/work-orders` | List assigned work orders |
| PATCH | `/api/service-platform/providers/me/work-orders/{id}/status` | Update work order status |

### 6.3 Resident APIs (authenticated users)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/service-platform/domains` | List active domains |
| GET | `/api/service-platform/domains/{id}` | Get domain with its categories |
| GET | `/api/service-platform/categories` | List categories (filterable by domain) |
| GET | `/api/service-platform/categories/{id}` | Get category detail with custom field definitions |
| GET | `/api/service-platform/search` | Search providers/offerings (query, domain, category, area) |
| GET | `/api/service-platform/providers/{id}` | View provider profile and offerings |
| POST | `/api/service-platform/requests` | Submit a service request |
| GET | `/api/service-platform/requests/my` | List my service requests |
| GET | `/api/service-platform/requests/{id}` | Get request detail with work order |
| PATCH | `/api/service-platform/requests/{id}/cancel` | Cancel a pending request |

### 6.4 Pagination & Filtering

All list endpoints support:
- `page` (0-indexed), `size` (default 20, max 100)
- `sort` (e.g., `createdAt,desc`)
- Endpoint-specific filters as query params

Uses Spring Data `Pageable` and returns `Page<T>` wrapper matching existing patterns.

## 7. Integration Points

### 7.1 Vendor Module

`ServiceProvider.vendorId` is an optional FK to the existing `Vendor` entity. When a provider is a company with procurement/invoicing needs, they are linked. Individual freelancers (yoga trainer, tutor) operate without a vendor record.

### 7.2 Invoice/Billing Module

On `WorkOrder` completion:
1. `WorkOrderService` calculates final cost (base price + materials + adjustments)
2. Creates an `Invoice` via existing `InvoiceService`
3. Stores the `invoiceId` on the `WorkOrder`

### 7.3 Notification Module

Status change events trigger notifications via existing `NotificationService`:

| Event | Recipients | Channel |
|-------|-----------|---------|
| New request submitted | Matching providers in the area | PUSH, IN_APP |
| Provider assigned | Resident, Provider | PUSH, EMAIL, IN_APP |
| Work order status change | Resident | PUSH, IN_APP |
| Work order completed | Resident (for signoff) | PUSH, EMAIL, IN_APP |
| Provider verified | Provider | EMAIL, IN_APP |

### 7.4 Security

- New role: `SERVICE_PROVIDER` added to the existing role hierarchy
- All endpoints enforce community-scoped access (users can only see data for their community)
- Providers can only modify their own offerings and work orders
- Admin endpoints require `ADMIN` or `SUPER_ADMIN` role

## 8. Seed Data

Phase 1 includes a seed data SQL script that loads all 14 default service domains and their categories from the requirements:

- Home Services (19 categories)
- Fitness & Wellness (14 categories)
- Healthcare (13 categories)
- Education (11 categories)
- Sports Coaching (14 categories)
- Pet Care (9 categories)
- Events & Lifestyle (13 categories)
- Automobile Services (10 categories)
- Maintenance & AMC (13 categories)
- Child Care (7 categories)
- Elder Care (8 categories)
- Community Facility Maintenance (14 categories)
- Business & Professional Services (10 categories)

Total: ~155 categories across 13 domains, all config data — no code required.

## 9. Testing Strategy

| Layer | What | How |
|-------|------|-----|
| Unit | Service logic, status transitions, validation | JUnit 5 + Mockito, mock repositories |
| Repository slice | Custom queries, JSONB queries, pagination | `@DataJpaTest` with H2/PostgreSQL |
| Integration | Full request → assignment → work order → completion flow | `@SpringBootTest` with test containers or embedded DB |

Key test scenarios:
- Admin creates domain and categories with custom fields
- Provider registers, creates offerings with custom field values matching category schema
- Resident submits request, provider accepts, work order lifecycle completes
- Status transition validation (invalid transitions rejected)
- Community-scoped access (user A can't see community B's data)
- Search returns relevant providers filtered by category, area, availability

## 10. Out of Scope (Phase 1)

These are handled in later phases:
- Time slot / availability management (Phase 2)
- Dynamic pricing rules, quotes, discounts (Phase 3)
- AMC contracts, warranty tracking (Phase 4)
- Reviews, ratings, trust scores (Phase 5)
- AI matching, recommendations, demand prediction (Phase 6)
- Dashboards, KPI analytics (Phase 7)
- Material/parts inventory integration (Phase 8)
- Real-time chat between resident and provider (existing Chat module, wire up later)
- Payment gateway integration (existing finance module, wire up in Phase 3)
