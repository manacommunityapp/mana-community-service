-- Sports Insurance: seed partner and catalogue products for SPORTS_PA, SPORTS_EVENT_LIABILITY, SPORTS_TEAM_GROUP

-- Seed a sandbox partner for sports insurance (idempotent: skip if already exists)
INSERT INTO insurance_partner (code, legal_name, partner_type, status, disclosure_text, commercial_disclosure, created_by)
SELECT 'MANA_SPORTS_SANDBOX', 'Mana Sports Insurance (Sandbox)', 'REFERRAL_PARTNER', 'ACTIVE',
       'Sports insurance products shown here are indicative. Final premiums, terms and cover depend on the insurer''s underwriting.',
       'Mana facilitates comparison only and does not underwrite or guarantee any policy.',
       1
WHERE NOT EXISTS (SELECT 1 FROM insurance_partner WHERE code = 'MANA_SPORTS_SANDBOX');

-- Sports Personal Accident product
INSERT INTO insurance_product (partner_id, category, subtype, name)
SELECT p.id, 'SPORTS_PA', 'INDIVIDUAL', 'Sports Personal Accident Cover'
FROM insurance_partner p WHERE p.code = 'MANA_SPORTS_SANDBOX'
  AND NOT EXISTS (SELECT 1 FROM insurance_product WHERE category = 'SPORTS_PA' AND partner_id = p.id);

INSERT INTO insurance_product_version (product_id, version_no, data_source, verification_status, submitted_by,
  eligibility_json, coverage_json, benefits_json, exclusions_json)
SELECT ip.id, 1, 'SANDBOX', 'VERIFIED', 1,
  '{"minAge":5,"maxAge":70,"sports":"Cricket, Football, Badminton, Tennis, Kabaddi, Basketball, Swimming, Athletics"}',
  '{"sumInsuredOptions":[100000,250000,500000,1000000],"premiumIndicative":"from INR 299/year"}',
  '["Fracture & ligament injury cover","Physiotherapy & rehabilitation costs","Loss of income during recovery","Ambulance & emergency transport"]',
  '["Pre-existing injuries (first 12 months)","Professional/international competitive sports","Extreme sports without add-on"]'
FROM insurance_product ip
JOIN insurance_partner p ON ip.partner_id = p.id
WHERE p.code = 'MANA_SPORTS_SANDBOX' AND ip.category = 'SPORTS_PA'
  AND NOT EXISTS (SELECT 1 FROM insurance_product_version WHERE product_id = ip.id AND version_no = 1);

-- Sports Event Liability product
INSERT INTO insurance_product (partner_id, category, subtype, name)
SELECT p.id, 'SPORTS_EVENT_LIABILITY', 'EVENT', 'Sports Event & Tournament Liability'
FROM insurance_partner p WHERE p.code = 'MANA_SPORTS_SANDBOX'
  AND NOT EXISTS (SELECT 1 FROM insurance_product WHERE category = 'SPORTS_EVENT_LIABILITY' AND partner_id = p.id);

INSERT INTO insurance_product_version (product_id, version_no, data_source, verification_status, submitted_by,
  eligibility_json, coverage_json, benefits_json, exclusions_json)
SELECT ip.id, 1, 'SANDBOX', 'VERIFIED', 1,
  '{"minParticipants":1,"maxParticipants":10000,"eventTypes":"Tournament, League, Friendly, Exhibition, Community Sports Day"}',
  '{"sumInsuredOptions":[500000,1000000,2500000,5000000],"premiumIndicative":"from INR 1499/event"}',
  '["Third-party bodily injury liability","Property damage during event","Legal defence costs","Medical expenses for spectators"]',
  '["Wilful misconduct by organiser","Events without proper permits","Pre-existing structural issues at venue"]'
FROM insurance_product ip
JOIN insurance_partner p ON ip.partner_id = p.id
WHERE p.code = 'MANA_SPORTS_SANDBOX' AND ip.category = 'SPORTS_EVENT_LIABILITY'
  AND NOT EXISTS (SELECT 1 FROM insurance_product_version WHERE product_id = ip.id AND version_no = 1);

-- Team / Group Sports Cover product
INSERT INTO insurance_product (partner_id, category, subtype, name)
SELECT p.id, 'SPORTS_TEAM_GROUP', 'GROUP', 'Team & Club Group Sports Cover'
FROM insurance_partner p WHERE p.code = 'MANA_SPORTS_SANDBOX'
  AND NOT EXISTS (SELECT 1 FROM insurance_product WHERE category = 'SPORTS_TEAM_GROUP' AND partner_id = p.id);

INSERT INTO insurance_product_version (product_id, version_no, data_source, verification_status, submitted_by,
  eligibility_json, coverage_json, benefits_json, exclusions_json)
SELECT ip.id, 1, 'SANDBOX', 'VERIFIED', 1,
  '{"minMembers":2,"maxMembers":500,"types":"Team, Academy, Club, Association"}',
  '{"sumInsuredOptions":[250000,500000,1000000,2500000],"premiumIndicative":"from INR 199/person/year"}',
  '["Group PA cover for entire roster","Tournament-specific add-ons","Coaching & training session cover","Equipment damage cover"]',
  '["Members not on registered roster","Activities outside declared sport","Professional contracts with separate cover"]'
FROM insurance_product ip
JOIN insurance_partner p ON ip.partner_id = p.id
WHERE p.code = 'MANA_SPORTS_SANDBOX' AND ip.category = 'SPORTS_TEAM_GROUP'
  AND NOT EXISTS (SELECT 1 FROM insurance_product_version WHERE product_id = ip.id AND version_no = 1);
