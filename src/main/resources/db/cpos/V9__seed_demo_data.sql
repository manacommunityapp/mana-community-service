-- ============================================================
-- V9: Seed Demo Data for CPOS
-- CPOS — Community Property Operating System
-- ============================================================

-- 1. Demo Tenant & Community
INSERT INTO tenants (id, tenant_code, tenant_name, tenant_type, subscription_plan, city)
VALUES ('e1111111-1111-1111-1111-111111111111', 'PRESTIGE_GROUP', 'Prestige Group Property Management', 'ENTERPRISE', 'ENTERPRISE', 'Bengaluru');

INSERT INTO communities (id, tenant_id, community_code, community_name, community_type, total_units, total_towers, city)
VALUES ('c1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'MEADOWS', 'Prestige Palm Meadows Township', 'GATED_SOCIETY', 2847, 6, 'Bengaluru');

INSERT INTO towers (id, tenant_id, community_id, tower_code, tower_name, total_floors, total_units)
VALUES
('t1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'TOWER_A', 'North Tower (A)', 14, 480),
('t2222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'TOWER_B', 'South Tower (B)', 14, 480);

-- 2. Demo Users
INSERT INTO users (id, tenant_id, email, phone, first_name, last_name, display_name)
VALUES
('u1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'admin@cpos.dev', '+91 98000 11111', 'System', 'Admin', 'System Admin'),
('u2222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111', 'rajesh.sharma@example.com', '+91 98765 43210', 'Rajesh', 'Sharma', 'Rajesh Sharma'),
('u3333333-3333-3333-3333-333333333333', 'e1111111-1111-1111-1111-111111111111', 'suresh.kumar@example.com', '+91 98123 45678', 'Suresh', 'Kumar', 'Suresh Kumar');

-- 3. Demo Properties
INSERT INTO properties (id, tenant_id, community_id, tower_id, property_code, property_type_code, unit_number, floor_number, carpet_area, bedrooms, bathrooms, occupancy_status, property_status, current_market_value, monthly_maintenance)
VALUES
('p1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 't1111111-1111-1111-1111-111111111111', 'CPOS-MEADOWS-A101', 'APARTMENT', 'A-101', 1, 1650.00, 3, 3, 'OWNER_OCCUPIED', 'ACTIVE', 14500000.00, 4500.00),
('p2222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 't2222222-2222-2222-2222-222222222222', 'CPOS-MEADOWS-B302', 'APARTMENT', 'B-302', 3, 1200.00, 2, 2, 'TENANT_OCCUPIED', 'ACTIVE', 11000000.00, 3800.00),
('p3333333-3333-3333-3333-333333333333', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 't1111111-1111-1111-1111-111111111111', 'CPOS-MEADOWS-VILLA14', 'VILLA', 'Villa-14', 0, 3200.00, 4, 5, 'OWNER_OCCUPIED', 'ACTIVE', 38000000.00, 9500.00);

-- 4. Demo Owners & Occupants
INSERT INTO property_owners (id, tenant_id, property_id, user_id, owner_type, full_name, purchase_date, purchase_price)
VALUES
('o1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', 'u2222222-2222-2222-2222-222222222222', 'INDIVIDUAL', 'Rajesh Sharma', '2021-03-15', 12500000.00);

INSERT INTO residents (id, tenant_id, community_id, property_id, user_id, resident_type, full_name, phone, rfid_card_number)
VALUES
('r1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', 'u2222222-2222-2222-2222-222222222222', 'OWNER', 'Rajesh Sharma', '+91 98765 43210', 'RFID-99201'),
('r2222222-2222-2222-2222-222222222222', 'e1111111-1111-1111-1111-111111111111', 'c1111111-1111-1111-1111-111111111111', 'p2222222-2222-2222-2222-222222222222', 'u3333333-3333-3333-3333-333333333333', 'TENANT', 'Suresh Kumar', '+91 98123 45678', 'RFID-88102');

-- 5. AI Valuations & Investment Scores
INSERT INTO ai_property_valuations (id, tenant_id, property_id, predicted_value, confidence_score, appreciation_1yr_pct)
VALUES
('v1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', 15600000.00, 94.00, 7.80);

INSERT INTO ai_investment_scores (id, tenant_id, property_id, overall_score, capital_appreciation_score, rental_yield_score, recommendation)
VALUES
('s1111111-1111-1111-1111-111111111111', 'e1111111-1111-1111-1111-111111111111', 'p1111111-1111-1111-1111-111111111111', 87.00, 8.50, 6.20, 'STRONG_BUY');
