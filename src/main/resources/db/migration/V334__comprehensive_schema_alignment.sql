-- V334: Comprehensive Schema Alignment for Governance, Group Buying, Safety, HRM,
-- MW, KYC, Marketplace, Commerce, Community, Feed, IoT Smart Meters, SMS Notification, and VMS entities.
-- All DDL is backwards-compatible and idempotent.

-- 1. Governance Vault Document
ALTER TABLE IF EXISTS manacommunity.governance_vault_document
    ADD COLUMN IF NOT EXISTS file_size_kb BIGINT,
    ADD COLUMN IF NOT EXISTS file_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS uploaded_by BIGINT;

-- 2. Group Buy Order
ALTER TABLE IF EXISTS manacommunity.group_buy_order
    ADD COLUMN IF NOT EXISTS escrow_hold_amount NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS delivery_address TEXT,
    ADD COLUMN IF NOT EXISTS special_notes TEXT,
    ADD COLUMN IF NOT EXISTS collector_pin VARCHAR(10),
    ADD COLUMN IF NOT EXISTS collector_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS collector_relation VARCHAR(50);

-- 3. Guard Shift
ALTER TABLE IF EXISTS manacommunity.guard_shift
    ADD COLUMN IF NOT EXISTS area VARCHAR(100),
    ADD COLUMN IF NOT EXISTS checkpoints_completed INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_checkpoints INT DEFAULT 0;

-- 4. HRM Appraisal Cycles
ALTER TABLE IF EXISTS manacommunity.hr_appraisal_cycles
    ADD COLUMN IF NOT EXISTS appraisal_start DATE,
    ADD COLUMN IF NOT EXISTS appraisal_end DATE;

-- 5. HRM Appraisal Forms
ALTER TABLE IF EXISTS manacommunity.hr_appraisal_forms
    ADD COLUMN IF NOT EXISTS reviewer_employee_id BIGINT,
    ADD COLUMN IF NOT EXISTS final_comments TEXT;

-- 6. HRM Career Paths
ALTER TABLE IF EXISTS manacommunity.hr_career_paths
    ADD COLUMN IF NOT EXISTS target_date DATE,
    ADD COLUMN IF NOT EXISTS development_activities TEXT;

-- 7. HRM Certifications
ALTER TABLE IF EXISTS manacommunity.hr_certifications
    ADD COLUMN IF NOT EXISTS name VARCHAR(300),
    ADD COLUMN IF NOT EXISTS issuing_body VARCHAR(200),
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE;

-- 8. HRM Feedback Responses
ALTER TABLE IF EXISTS manacommunity.hr_feedback_responses
    ADD COLUMN IF NOT EXISTS reviewer_employee_id BIGINT,
    ADD COLUMN IF NOT EXISTS general_comments TEXT;

-- 9. HRM Interviews
ALTER TABLE IF EXISTS manacommunity.hr_interviews
    ADD COLUMN IF NOT EXISTS location VARCHAR(200),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'SCHEDULED';

-- 10. HRM Job Postings
ALTER TABLE IF EXISTS manacommunity.hr_job_postings
    ADD COLUMN IF NOT EXISTS department_id BIGINT;

-- 11. HRM Offers
ALTER TABLE IF EXISTS manacommunity.hr_offers
    ADD COLUMN IF NOT EXISTS proposed_joining_date DATE,
    ADD COLUMN IF NOT EXISTS offer_letter_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS expiry_date DATE,
    ADD COLUMN IF NOT EXISTS response_date DATE;

-- 12. HRM Applicants
ALTER TABLE IF EXISTS manacommunity.hr_applicants
    ADD COLUMN IF NOT EXISTS current_employer VARCHAR(200),
    ADD COLUMN IF NOT EXISTS experience_years INT,
    ADD COLUMN IF NOT EXISTS expected_salary DOUBLE PRECISION;

-- 13. MW Duty Handovers
ALTER TABLE IF EXISTS manacommunity.mw_duty_handovers
    ADD COLUMN IF NOT EXISTS handover_from BIGINT,
    ADD COLUMN IF NOT EXISTS handover_to BIGINT;

-- 14. KYC Document
ALTER TABLE IF EXISTS manacommunity.kyc_document
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT,
    ADD COLUMN IF NOT EXISTS community_id BIGINT,
    ADD COLUMN IF NOT EXISTS document_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS document_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS front_image_url TEXT,
    ADD COLUMN IF NOT EXISTS back_image_url TEXT;

-- 15-17. Marketplace Audit Columns
ALTER TABLE IF EXISTS manacommunity.market_inventory_reservations
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.market_seller_profiles
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.market_settlement_records
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- 18-24. Commerce Audit and Optimistic Locking Columns
ALTER TABLE IF EXISTS manacommunity.commerce_customer_profile
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.commerce_dispute
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.commerce_payment
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.commerce_review
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.commerce_settlement
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.commerce_flash_sale_item
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

ALTER TABLE IF EXISTS manacommunity.commerce_loyalty_account
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- 25-28. Community & Resident Audit Columns
ALTER TABLE IF EXISTS manacommunity.community_flat
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.flat_membership
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.flat_dependent_member
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

ALTER TABLE IF EXISTS manacommunity.community_graph_profile
    ADD COLUMN IF NOT EXISTS created_by BIGINT,
    ADD COLUMN IF NOT EXISTS updated_by BIGINT;

-- 29. Content Report Columns
ALTER TABLE IF EXISTS manacommunity.content_report
    ADD COLUMN IF NOT EXISTS reported_by BIGINT,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS content_id BIGINT,
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS reviewed_by BIGINT,
    ADD COLUMN IF NOT EXISTS review_note VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

-- 30-32. Feed & Hashtag Columns
ALTER TABLE IF EXISTS manacommunity.post
    ADD COLUMN IF NOT EXISTS is_official BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS moderation_note VARCHAR(1000);

ALTER TABLE IF EXISTS manacommunity.post_comment
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_accepted_answer BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS manacommunity.hashtag
    ADD COLUMN IF NOT EXISTS is_trending BOOLEAN DEFAULT FALSE;

-- 33. Smart Meters Telemetry Columns
ALTER TABLE IF EXISTS manacommunity.smart_meters
    ADD COLUMN IF NOT EXISTS flat_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS meter_serial VARCHAR(50),
    ADD COLUMN IF NOT EXISTS location VARCHAR(100),
    ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_reading_date DATE;

-- 34-39. SMS & Notification Service Columns
ALTER TABLE IF EXISTS manacommunity.sms_message
    ADD COLUMN IF NOT EXISTS provider_name VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_response TEXT,
    ADD COLUMN IF NOT EXISTS segments INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS campaign_id BIGINT,
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS reference_id BIGINT;

ALTER TABLE IF EXISTS manacommunity.sms_delivery_event
    ADD COLUMN IF NOT EXISTS sms_message_id BIGINT,
    ADD COLUMN IF NOT EXISTS raw_payload TEXT,
    ADD COLUMN IF NOT EXISTS event_timestamp TIMESTAMP;

ALTER TABLE IF EXISTS manacommunity.sms_cost_record
    ADD COLUMN IF NOT EXISTS provider_name VARCHAR(30),
    ADD COLUMN IF NOT EXISTS provider_message_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS to_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS segments INT DEFAULT 1,
    ADD COLUMN IF NOT EXISTS cost_usd NUMERIC(12, 6) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cost_inr NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS billing_date DATE,
    ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS manacommunity.bulk_sms_campaign
    ADD COLUMN IF NOT EXISTS name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS total_recipients INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delivered_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS approved_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS estimated_cost_usd NUMERIC(12, 4),
    ADD COLUMN IF NOT EXISTS actual_cost_usd NUMERIC(12, 4) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS template_variables TEXT;

ALTER TABLE IF EXISTS manacommunity.sms_otp_transaction
    ADD COLUMN IF NOT EXISTS client_ip VARCHAR(45),
    ADD COLUMN IF NOT EXISTS sms_message_id BIGINT;

ALTER TABLE IF EXISTS manacommunity.sms_template
    ADD COLUMN IF NOT EXISTS name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS body TEXT,
    ADD COLUMN IF NOT EXISTS dlt_template_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS max_length INT,
    ADD COLUMN IF NOT EXISTS is_unicode BOOLEAN DEFAULT FALSE;

-- 40-41. Privacy & Notification Preferences
ALTER TABLE IF EXISTS manacommunity.user_privacy_settings
    ADD COLUMN IF NOT EXISTS show_vehicle_in_directory BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS emergency_contact_restricted BOOLEAN DEFAULT TRUE;

ALTER TABLE IF EXISTS manacommunity.user_sms_preference
    ADD COLUMN IF NOT EXISTS notification_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS whatsapp_enabled BOOLEAN DEFAULT FALSE;

-- 42-52. Vendor Management System (VMS) Columns
ALTER TABLE IF EXISTS manacommunity.vms_ratings
    ADD COLUMN IF NOT EXISTS comment VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS vendor_reply VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS vendor_replied_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reported BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS report_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS is_anonymous BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS manacommunity.vms_reviews
    ADD COLUMN IF NOT EXISTS vendor_id BIGINT,
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS content VARCHAR(5000),
    ADD COLUMN IF NOT EXISTS pros VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS cons VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS is_verified_purchase BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS vendor_reply VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS vendor_reply_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS community_id BIGINT;

ALTER TABLE IF EXISTS manacommunity.vms_review_media
    ADD COLUMN IF NOT EXISTS media_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS caption VARCHAR(200);

ALTER TABLE IF EXISTS manacommunity.vms_vendor_performance
    ADD COLUMN IF NOT EXISTS avg_rating DECIMAL(3,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_ratings INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_bookings INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS completed_bookings INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancelled_bookings INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS on_time_completion_rate DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS response_time_minutes INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_revenue DECIMAL(14,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS performance_score DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS performance_tier VARCHAR(20) DEFAULT 'BRONZE',
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS manacommunity.vms_vendor_performance_snapshots
    ADD COLUMN IF NOT EXISTS period_type VARCHAR(20) DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS bookings_count INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS revenue DECIMAL(14,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS avg_rating DECIMAL(3,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS completion_rate DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS on_time_rate DECIMAL(5,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS new_customers INT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS repeat_customers INT DEFAULT 0;

ALTER TABLE IF EXISTS manacommunity.vms_notification_logs
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS manacommunity.vms_notification_preferences
    ADD COLUMN IF NOT EXISTS notification_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS channel VARCHAR(30),
    ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE IF EXISTS manacommunity.vms_notification_templates
    ADD COLUMN IF NOT EXISTS code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';

ALTER TABLE IF EXISTS manacommunity.vms_ai_recommendations
    ADD COLUMN IF NOT EXISTS recommendation_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS confidence_score DECIMAL(5,4),
    ADD COLUMN IF NOT EXISTS is_clicked BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_booked BOOLEAN DEFAULT FALSE;

ALTER TABLE IF EXISTS manacommunity.vms_ai_predictions
    ADD COLUMN IF NOT EXISTS vendor_id BIGINT,
    ADD COLUMN IF NOT EXISTS confidence_score DECIMAL(5,4),
    ADD COLUMN IF NOT EXISTS input_features TEXT;

ALTER TABLE IF EXISTS manacommunity.vms_audit_log
    ADD COLUMN IF NOT EXISTS old_values TEXT,
    ADD COLUMN IF NOT EXISTS new_values TEXT,
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);

-- Safe backfill and bridging of legacy column data where applicable
DO $$
BEGIN
  IF to_regclass('manacommunity.hr_appraisal_cycles') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_appraisal_cycles' AND column_name='appraisal_window_start') THEN
      EXECUTE 'UPDATE manacommunity.hr_appraisal_cycles SET appraisal_start = appraisal_window_start WHERE appraisal_start IS NULL AND appraisal_window_start IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_appraisal_cycles' AND column_name='appraisal_window_end') THEN
      EXECUTE 'UPDATE manacommunity.hr_appraisal_cycles SET appraisal_end = appraisal_window_end WHERE appraisal_end IS NULL AND appraisal_window_end IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.hr_appraisal_forms') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_appraisal_forms' AND column_name='reviewer_id') THEN
      EXECUTE 'UPDATE manacommunity.hr_appraisal_forms SET reviewer_employee_id = reviewer_id WHERE reviewer_employee_id IS NULL AND reviewer_id IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.hr_certifications') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_certifications' AND column_name='certification_name') THEN
      EXECUTE 'UPDATE manacommunity.hr_certifications SET name = certification_name WHERE name IS NULL AND certification_name IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_certifications' AND column_name='issuing_organization') THEN
      EXECUTE 'UPDATE manacommunity.hr_certifications SET issuing_body = issuing_organization WHERE issuing_body IS NULL AND issuing_organization IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.hr_feedback_responses') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_feedback_responses' AND column_name='reviewer_id') THEN
      EXECUTE 'UPDATE manacommunity.hr_feedback_responses SET reviewer_employee_id = reviewer_id WHERE reviewer_employee_id IS NULL AND reviewer_id IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_feedback_responses' AND column_name='additional_comments') THEN
      EXECUTE 'UPDATE manacommunity.hr_feedback_responses SET general_comments = additional_comments WHERE general_comments IS NULL AND additional_comments IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.hr_interviews') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_interviews' AND column_name='location_or_link') THEN
      EXECUTE 'UPDATE manacommunity.hr_interviews SET location = location_or_link WHERE location IS NULL AND location_or_link IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.hr_offers') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_offers' AND column_name='joining_date') THEN
      EXECUTE 'UPDATE manacommunity.hr_offers SET proposed_joining_date = joining_date WHERE proposed_joining_date IS NULL AND joining_date IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_offers' AND column_name='offer_expiry_date') THEN
      EXECUTE 'UPDATE manacommunity.hr_offers SET expiry_date = offer_expiry_date WHERE expiry_date IS NULL AND offer_expiry_date IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='hr_offers' AND column_name='responded_at') THEN
      EXECUTE 'UPDATE manacommunity.hr_offers SET response_date = responded_at::date WHERE response_date IS NULL AND responded_at IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.mw_duty_handovers') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='mw_duty_handovers' AND column_name='handover_from_id') THEN
      EXECUTE 'UPDATE manacommunity.mw_duty_handovers SET handover_from = handover_from_id WHERE handover_from IS NULL AND handover_from_id IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='mw_duty_handovers' AND column_name='handover_to_id') THEN
      EXECUTE 'UPDATE manacommunity.mw_duty_handovers SET handover_to = handover_to_id WHERE handover_to IS NULL AND handover_to_id IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.kyc_document') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='kyc_document' AND column_name='id_type') THEN
      EXECUTE 'UPDATE manacommunity.kyc_document SET document_type = id_type WHERE document_type IS NULL AND id_type IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='kyc_document' AND column_name='id_number') THEN
      EXECUTE 'UPDATE manacommunity.kyc_document SET document_number = id_number WHERE document_number IS NULL AND id_number IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='kyc_document' AND column_name='front_url') THEN
      EXECUTE 'UPDATE manacommunity.kyc_document SET front_image_url = front_url WHERE front_image_url IS NULL AND front_url IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='kyc_document' AND column_name='back_url') THEN
      EXECUTE 'UPDATE manacommunity.kyc_document SET back_image_url = back_url WHERE back_image_url IS NULL AND back_url IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='kyc_document' AND column_name='submitted_at') THEN
      EXECUTE 'UPDATE manacommunity.kyc_document SET created_at = submitted_at WHERE created_at IS NULL AND submitted_at IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.content_report') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='content_report' AND column_name='reporter_id') THEN
      EXECUTE 'UPDATE manacommunity.content_report SET reported_by = reporter_id WHERE reported_by IS NULL AND reporter_id IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='content_report' AND column_name='target_type') THEN
      EXECUTE 'UPDATE manacommunity.content_report SET content_type = target_type WHERE content_type IS NULL AND target_type IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='content_report' AND column_name='target_id') THEN
      EXECUTE 'UPDATE manacommunity.content_report SET content_id = target_id WHERE content_id IS NULL AND target_id IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='content_report' AND column_name='resolved_by_id') THEN
      EXECUTE 'UPDATE manacommunity.content_report SET reviewed_by = resolved_by_id WHERE reviewed_by IS NULL AND resolved_by_id IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='content_report' AND column_name='resolved_at') THEN
      EXECUTE 'UPDATE manacommunity.content_report SET reviewed_at = resolved_at WHERE reviewed_at IS NULL AND resolved_at IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.post') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post' AND column_name='official') THEN
      EXECUTE 'UPDATE manacommunity.post SET is_official = official WHERE is_official IS NULL AND official IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post' AND column_name='pinned') THEN
      EXECUTE 'UPDATE manacommunity.post SET is_pinned = pinned WHERE is_pinned IS NULL AND pinned IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post' AND column_name='deleted') THEN
      EXECUTE 'UPDATE manacommunity.post SET is_deleted = deleted WHERE is_deleted IS NULL AND deleted IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.post_comment') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post_comment' AND column_name='pinned') THEN
      EXECUTE 'UPDATE manacommunity.post_comment SET is_pinned = pinned WHERE is_pinned IS NULL AND pinned IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post_comment' AND column_name='accepted_answer') THEN
      EXECUTE 'UPDATE manacommunity.post_comment SET is_accepted_answer = accepted_answer WHERE is_accepted_answer IS NULL AND accepted_answer IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='post_comment' AND column_name='deleted') THEN
      EXECUTE 'UPDATE manacommunity.post_comment SET is_deleted = deleted WHERE is_deleted IS NULL AND deleted IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.smart_meters') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='smart_meters' AND column_name='unit_number') THEN
      EXECUTE 'UPDATE manacommunity.smart_meters SET flat_number = unit_number WHERE flat_number IS NULL AND unit_number IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='smart_meters' AND column_name='meter_serial_number') THEN
      EXECUTE 'UPDATE manacommunity.smart_meters SET meter_serial = meter_serial_number WHERE meter_serial IS NULL AND meter_serial_number IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.sms_message') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_message' AND column_name='provider') THEN
      EXECUTE 'UPDATE manacommunity.sms_message SET provider_name = provider WHERE provider_name IS NULL AND provider IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.sms_delivery_event') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_delivery_event' AND column_name='payload') THEN
      EXECUTE 'UPDATE manacommunity.sms_delivery_event SET raw_payload = payload WHERE raw_payload IS NULL AND payload IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.sms_cost_record') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_cost_record' AND column_name='provider') THEN
      EXECUTE 'UPDATE manacommunity.sms_cost_record SET provider_name = provider WHERE provider_name IS NULL AND provider IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_cost_record' AND column_name='message_units') THEN
      EXECUTE 'UPDATE manacommunity.sms_cost_record SET segments = message_units WHERE segments IS NULL AND message_units IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_cost_record' AND column_name='created_at') THEN
      EXECUTE 'UPDATE manacommunity.sms_cost_record SET recorded_at = created_at WHERE recorded_at IS NULL AND created_at IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.bulk_sms_campaign') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='bulk_sms_campaign' AND column_name='campaign_reference') THEN
      EXECUTE 'UPDATE manacommunity.bulk_sms_campaign SET name = campaign_reference WHERE name IS NULL AND campaign_reference IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='bulk_sms_campaign' AND column_name='recipient_count') THEN
      EXECUTE 'UPDATE manacommunity.bulk_sms_campaign SET total_recipients = recipient_count WHERE total_recipients IS NULL AND recipient_count IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='bulk_sms_campaign' AND column_name='created_by') THEN
      EXECUTE 'UPDATE manacommunity.bulk_sms_campaign SET created_by_user_id = created_by WHERE created_by_user_id IS NULL AND created_by IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='bulk_sms_campaign' AND column_name='approved_by') THEN
      EXECUTE 'UPDATE manacommunity.bulk_sms_campaign SET approved_by_user_id = approved_by WHERE approved_by_user_id IS NULL AND approved_by IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='bulk_sms_campaign' AND column_name='variables') THEN
      EXECUTE 'UPDATE manacommunity.bulk_sms_campaign SET template_variables = variables WHERE template_variables IS NULL AND variables IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.sms_template') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_template' AND column_name='template_name') THEN
      EXECUTE 'UPDATE manacommunity.sms_template SET name = template_name WHERE name IS NULL AND template_name IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_template' AND column_name='content') THEN
      EXECUTE 'UPDATE manacommunity.sms_template SET body = content WHERE body IS NULL AND content IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='sms_template' AND column_name='provider_template_id') THEN
      EXECUTE 'UPDATE manacommunity.sms_template SET dlt_template_id = provider_template_id WHERE dlt_template_id IS NULL AND provider_template_id IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_reviews') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_reviews' AND column_name='comment') THEN
      EXECUTE 'UPDATE manacommunity.vms_reviews SET content = comment WHERE content IS NULL AND comment IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_review_media') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_review_media' AND column_name='url') THEN
      EXECUTE 'UPDATE manacommunity.vms_review_media SET media_url = url WHERE media_url IS NULL AND url IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_vendor_performance') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_vendor_performance' AND column_name='average_rating') THEN
      EXECUTE 'UPDATE manacommunity.vms_vendor_performance SET avg_rating = average_rating WHERE avg_rating IS NULL AND average_rating IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_vendor_performance' AND column_name='jobs_completed') THEN
      EXECUTE 'UPDATE manacommunity.vms_vendor_performance SET completed_bookings = jobs_completed WHERE completed_bookings IS NULL AND jobs_completed IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_vendor_performance' AND column_name='jobs_cancelled') THEN
      EXECUTE 'UPDATE manacommunity.vms_vendor_performance SET cancelled_bookings = jobs_cancelled WHERE cancelled_bookings IS NULL AND jobs_cancelled IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_vendor_performance' AND column_name='sla_compliance_percent') THEN
      EXECUTE 'UPDATE manacommunity.vms_vendor_performance SET on_time_completion_rate = sla_compliance_percent WHERE on_time_completion_rate IS NULL AND sla_compliance_percent IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_vendor_performance' AND column_name='average_response_minutes') THEN
      EXECUTE 'UPDATE manacommunity.vms_vendor_performance SET response_time_minutes = average_response_minutes WHERE response_time_minutes IS NULL AND average_response_minutes IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_vendor_performance' AND column_name='revenue') THEN
      EXECUTE 'UPDATE manacommunity.vms_vendor_performance SET total_revenue = revenue WHERE total_revenue IS NULL AND revenue IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_notification_preferences') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_notification_preferences' AND column_name='event_type') THEN
      EXECUTE 'UPDATE manacommunity.vms_notification_preferences SET notification_type = event_type WHERE notification_type IS NULL AND event_type IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_notification_templates') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_notification_templates' AND column_name='event_type') THEN
      EXECUTE 'UPDATE manacommunity.vms_notification_templates SET code = event_type WHERE code IS NULL AND event_type IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_notification_templates' AND column_name='subject') THEN
      EXECUTE 'UPDATE manacommunity.vms_notification_templates SET name = subject WHERE name IS NULL AND subject IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_ai_recommendations') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_ai_recommendations' AND column_name='context_type') THEN
      EXECUTE 'UPDATE manacommunity.vms_ai_recommendations SET recommendation_type = context_type WHERE recommendation_type IS NULL AND context_type IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_ai_recommendations' AND column_name='score') THEN
      EXECUTE 'UPDATE manacommunity.vms_ai_recommendations SET confidence_score = score WHERE confidence_score IS NULL AND score IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_ai_predictions') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_ai_predictions' AND column_name='entity_id') THEN
      EXECUTE 'UPDATE manacommunity.vms_ai_predictions SET vendor_id = entity_id WHERE vendor_id IS NULL AND entity_id IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_ai_predictions' AND column_name='confidence') THEN
      EXECUTE 'UPDATE manacommunity.vms_ai_predictions SET confidence_score = confidence WHERE confidence_score IS NULL AND confidence IS NOT NULL';
    END IF;
  END IF;

  IF to_regclass('manacommunity.vms_audit_log') IS NOT NULL THEN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_audit_log' AND column_name='old_value') THEN
      EXECUTE 'UPDATE manacommunity.vms_audit_log SET old_values = old_value WHERE old_values IS NULL AND old_value IS NOT NULL';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='manacommunity' AND table_name='vms_audit_log' AND column_name='new_value') THEN
      EXECUTE 'UPDATE manacommunity.vms_audit_log SET new_values = new_value WHERE new_values IS NULL AND new_value IS NOT NULL';
    END IF;
  END IF;
END $$;
