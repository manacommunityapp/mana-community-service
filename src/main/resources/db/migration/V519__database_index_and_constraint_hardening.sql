-- ============================================================================
-- V519: Database Index & Constraint Hardening
--
-- Applies the verified findings from the schema architecture audit:
--   1. Adds missing payment webhook idempotency & lookup indexes (cashfree_order,
--      payment_order, transaction_core_record).
--   2. Adds missing JPA @Index definitions not previously materialized in Flyway.
--   3. Adds missing tenant (community_id) & Foreign Key B-Tree indexes.
--   4. Safely drops verified redundant single-column/duplicate indexes only when
--      their covering index exists and is not backing a pg_constraint.
--   5. Resolves V68 vs V334 NOT NULL / UNIQUE conflicts on sms_message and
--      user_sms_preference.
--   6. Adds upcoming monthly partitions for sensor_telemetry (V480).
-- ============================================================================

-- ── 1. Add Missing Critical, Tenant, JPA, and Foreign Key Indexes ─────────
DO $$
DECLARE
    rec RECORD;
    col_arr TEXT[];
    c TEXT;
    col_only TEXT;
    all_cols_exist BOOLEAN;
    already_covered BOOLEAN;
    stmt TEXT;
BEGIN
    FOR rec IN SELECT * FROM (VALUES
        ('idx_cf_order_webhook_event', 'cashfree_order', 'webhook_event_id', TRUE, 'webhook_event_id IS NOT NULL'),
        ('idx_cf_order_cf_payment_id', 'cashfree_order', 'cf_payment_id', FALSE, 'cf_payment_id IS NOT NULL'),
        ('idx_cf_order_cf_order_id', 'cashfree_order', 'cf_order_id', FALSE, 'cf_order_id IS NOT NULL'),
        ('idx_payment_order_gw_payment_id', 'payment_order', 'gateway_payment_id', FALSE, 'gateway_payment_id IS NOT NULL'),
        ('idx_payment_order_gw_order_id', 'payment_order', 'gateway_order_id', FALSE, 'gateway_order_id IS NOT NULL'),
        ('idx_payment_order_ledger_entry', 'payment_order', 'ledger_entry_id', FALSE, 'ledger_entry_id IS NOT NULL'),
        ('idx_txn_core_comm_status_created', 'transaction_core_record', 'community_id, status, created_at desc', FALSE, NULL),
        ('idx_txn_core_idempotency_uq', 'transaction_core_record', 'idempotency_key', TRUE, 'idempotency_key IS NOT NULL'),
        ('idx_txn_core_payment_id', 'transaction_core_record', 'payment_id', FALSE, 'payment_id IS NOT NULL'),
        ('idx_txn_core_invoice_id', 'transaction_core_record', 'invoice_id', FALSE, 'invoice_id IS NOT NULL'),
        ('idx_txn_core_receipt_id', 'transaction_core_record', 'receipt_id', FALSE, 'receipt_id IS NOT NULL'),
        ('idx_txn_core_journal_entry_id', 'transaction_core_record', 'journal_entry_id', FALSE, 'journal_entry_id IS NOT NULL'),
        ('idx_txn_core_settlement_id', 'transaction_core_record', 'settlement_id', FALSE, 'settlement_id IS NOT NULL'),
        ('idx_txn_core_refund_id', 'transaction_core_record', 'refund_id', FALSE, 'refund_id IS NOT NULL'),
        ('idx_audit_log_user_id_created_at', 'audit_log', 'user_id, created_at', FALSE, NULL),
        ('idx_commute_ride_community_id_departure_time', 'commute_ride', 'community_id, departure_time desc', FALSE, NULL),
        ('idx_jewellery_tryon_session_user_id_started_at', 'jewellery_tryon_session', 'user_id, started_at', FALSE, NULL),
        ('idx_marketplace_listings_community_id', 'marketplace_listings', 'community_id', FALSE, NULL),
        ('idx_marketplace_orders_order_number', 'marketplace_orders', 'order_number', FALSE, NULL),
        ('idx_mobility_driver_offer_driver_id_offered_at', 'mobility_driver_offer', 'driver_id, offered_at desc', FALSE, NULL),
        ('idx_mobility_trip_passenger_id_requested_at', 'mobility_trip', 'passenger_id, requested_at desc', FALSE, NULL),
        ('idx_notification_user_id_is_read', 'notification', 'user_id, is_read', FALSE, NULL),
        ('idx_post_community_id_created_at', 'post', 'community_id, created_at desc', FALSE, NULL),
        ('idx_resource_community_id', 'resource', 'community_id', FALSE, NULL),
        ('idx_resource_booking_resource_id', 'resource_booking', 'resource_id', FALSE, NULL),
        ('idx_service_provider_user_id', 'service_provider', 'user_id', FALSE, NULL),
        ('idx_service_request_requester_id', 'service_request', 'requester_id', FALSE, NULL),
        ('idx_sports_event_uuid', 'sports_event', 'uuid', FALSE, NULL),
        ('idx_work_order_service_request_id', 'work_order', 'service_request_id', FALSE, NULL),
        ('idx_coach_package_enrollment_community_id', 'coach_package_enrollment', 'community_id', FALSE, NULL),
        ('idx_commerce_products_community_id', 'commerce_products', 'community_id', FALSE, NULL),
        ('idx_community_insights_community_id', 'community_insights', 'community_id', FALSE, NULL),
        ('idx_community_resident_wallets_community_id', 'community_resident_wallets', 'community_id', FALSE, NULL),
        ('idx_community_seller_reviews_community_id', 'community_seller_reviews', 'community_id', FALSE, NULL),
        ('idx_community_trip_community_id', 'community_trip', 'community_id', FALSE, NULL),
        ('idx_data_deletion_request_community_id', 'data_deletion_request', 'community_id', FALSE, NULL),
        ('idx_delivery_assignments_community_id', 'delivery_assignments', 'community_id', FALSE, NULL),
        ('idx_delivery_batch_community_id', 'delivery_batch', 'community_id', FALSE, NULL),
        ('idx_delivery_slot_community_id', 'delivery_slot', 'community_id', FALSE, NULL),
        ('idx_delivery_slot_template_community_id', 'delivery_slot_template', 'community_id', FALSE, NULL),
        ('idx_device_control_log_community_id', 'device_control_log', 'community_id', FALSE, NULL),
        ('idx_device_visitor_otps_community_id', 'device_visitor_otps', 'community_id', FALSE, NULL),
        ('idx_email_theme_community_id', 'email_theme', 'community_id', FALSE, NULL),
        ('idx_event_activity_registrations_community_id', 'event_activity_registrations', 'community_id', FALSE, NULL),
        ('idx_event_department_community_id', 'event_department', 'community_id', FALSE, NULL),
        ('idx_event_meal_registrations_community_id', 'event_meal_registrations', 'community_id', FALSE, NULL),
        ('idx_event_pooja_schedule_community_id', 'event_pooja_schedule', 'community_id', FALSE, NULL),
        ('idx_event_pooja_slot_reservation_community_id', 'event_pooja_slot_reservation', 'community_id', FALSE, NULL),
        ('idx_event_registration_community_id', 'event_registration', 'community_id', FALSE, NULL),
        ('idx_hashtag_community_id', 'hashtag', 'community_id', FALSE, NULL),
        ('idx_hr_timeguard_anomaly_community_id', 'hr_timeguard_anomaly', 'community_id', FALSE, NULL),
        ('idx_hs_recurring_plan_community_id', 'hs_recurring_plan', 'community_id', FALSE, NULL),
        ('idx_hs_rework_request_community_id', 'hs_rework_request', 'community_id', FALSE, NULL),
        ('idx_hs_service_package_community_id', 'hs_service_package', 'community_id', FALSE, NULL),
        ('idx_hs_warranty_community_id', 'hs_warranty', 'community_id', FALSE, NULL),
        ('idx_hs_worker_community_id', 'hs_worker', 'community_id', FALSE, NULL),
        ('idx_hs_worker_availability_community_id', 'hs_worker_availability', 'community_id', FALSE, NULL),
        ('idx_hs_worker_payout_community_id', 'hs_worker_payout', 'community_id', FALSE, NULL),
        ('idx_idempotency_key_community_id', 'idempotency_key', 'community_id', FALSE, NULL),
        ('idx_insurance_claims_assistance_case_community_id', 'insurance_claims_assistance_case', 'community_id', FALSE, NULL),
        ('idx_insurance_group_enquiry_community_id', 'insurance_group_enquiry', 'community_id', FALSE, NULL),
        ('idx_insurance_policy_community_id', 'insurance_policy', 'community_id', FALSE, NULL),
        ('idx_insurance_quote_request_community_id', 'insurance_quote_request', 'community_id', FALSE, NULL),
        ('idx_invoice_category_community_id', 'invoice_category', 'community_id', FALSE, NULL),
        ('idx_khata_parties_community_id', 'khata_parties', 'community_id', FALSE, NULL),
        ('idx_kyc_document_community_id', 'kyc_document', 'community_id', FALSE, NULL),
        ('idx_media_upload_sessions_community_id', 'media_upload_sessions', 'community_id', FALSE, NULL),
        ('idx_merchandise_order_community_id', 'merchandise_order', 'community_id', FALSE, NULL),
        ('idx_move_checklist_item_community_id', 'move_checklist_item', 'community_id', FALSE, NULL),
        ('idx_outbox_event_community_id', 'outbox_event', 'community_id', FALSE, NULL),
        ('idx_platform_alerts_community_id', 'platform_alerts', 'community_id', FALSE, NULL),
        ('idx_privacy_audit_log_community_id', 'privacy_audit_log', 'community_id', FALSE, NULL),
        ('idx_product_stock_status_community_id', 'product_stock_status', 'community_id', FALSE, NULL),
        ('idx_product_substitute_community_id', 'product_substitute', 'community_id', FALSE, NULL),
        ('idx_recurring_basket_community_id', 'recurring_basket', 'community_id', FALSE, NULL),
        ('idx_sensor_anomaly_events_community_id', 'sensor_anomaly_events', 'community_id', FALSE, NULL),
        ('idx_sensor_telemetry_community_id', 'sensor_telemetry', 'community_id', FALSE, NULL),
        ('idx_sports_coach_community_id', 'sports_coach', 'community_id', FALSE, NULL),
        ('idx_staff_access_rule_community_id', 'staff_access_rule', 'community_id', FALSE, NULL),
        ('idx_staff_leave_community_id', 'staff_leave', 'community_id', FALSE, NULL),
        ('idx_tournament_merchandise_community_id', 'tournament_merchandise', 'community_id', FALSE, NULL),
        ('idx_tournament_sponsor_community_id', 'tournament_sponsor', 'community_id', FALSE, NULL),
        ('idx_trainer_slot_community_id', 'trainer_slot', 'community_id', FALSE, NULL),
        ('idx_trainer_slot_booking_community_id', 'trainer_slot_booking', 'community_id', FALSE, NULL),
        ('idx_transaction_intent_community_id', 'transaction_intent', 'community_id', FALSE, NULL),
        ('idx_transaction_reconciliation_log_community_id', 'transaction_reconciliation_log', 'community_id', FALSE, NULL),
        ('idx_user_engagement_score_community_id', 'user_engagement_score', 'community_id', FALSE, NULL),
        ('idx_user_location_consent_community_id', 'user_location_consent', 'community_id', FALSE, NULL),
        ('idx_vehicle_service_slot_community_id', 'vehicle_service_slot', 'community_id', FALSE, NULL),
        ('idx_vendor_prep_analytics_community_id', 'vendor_prep_analytics', 'community_id', FALSE, NULL),
        ('idx_vms_reviews_community_id', 'vms_reviews', 'community_id', FALSE, NULL),
        ('idx_wellness_challenge_community_id', 'wellness_challenge', 'community_id', FALSE, NULL),
        ('idx_wellness_provider_community_id', 'wellness_provider', 'community_id', FALSE, NULL),
        ('idx_announcement_author_id', 'announcement', 'author_id', FALSE, NULL),
        ('idx_asset_maintenance_log_schedule_id', 'asset_maintenance_log', 'schedule_id', FALSE, NULL),
        ('idx_ballot_vote_option_id', 'ballot_vote', 'option_id', FALSE, NULL),
        ('idx_batting_entry_scorecard_id', 'batting_entry', 'scorecard_id', FALSE, NULL),
        ('idx_biometric_access_logs_turnstile_id', 'biometric_access_logs', 'turnstile_id', FALSE, NULL),
        ('idx_bowling_entry_scorecard_id', 'bowling_entry', 'scorecard_id', FALSE, NULL),
        ('idx_buying_group_leader_id', 'buying_group', 'leader_id', FALSE, NULL),
        ('idx_cfbos_expense_mana_vendor_id', 'cfbos_expense', 'mana_vendor_id', FALSE, NULL),
        ('idx_commerce_bulk_order_vendor_user_id', 'commerce_bulk_order', 'vendor_user_id', FALSE, NULL),
        ('idx_commerce_bulk_order_approved_by_id', 'commerce_bulk_order', 'approved_by_id', FALSE, NULL),
        ('idx_commerce_bulk_order_fulfilled_order_id', 'commerce_bulk_order', 'fulfilled_order_id', FALSE, NULL),
        ('idx_commerce_cart_items_cart_id', 'commerce_cart_items', 'cart_id', FALSE, NULL),
        ('idx_commerce_coupon_vendor_user_id', 'commerce_coupon', 'vendor_user_id', FALSE, NULL),
        ('idx_commerce_coupon_created_by_id', 'commerce_coupon', 'created_by_id', FALSE, NULL),
        ('idx_commerce_coupon_usage_user_id', 'commerce_coupon_usage', 'user_id', FALSE, NULL),
        ('idx_commerce_coupon_usage_order_id', 'commerce_coupon_usage', 'order_id', FALSE, NULL),
        ('idx_commerce_dispute_order_id', 'commerce_dispute', 'order_id', FALSE, NULL),
        ('idx_commerce_dispute_user_id', 'commerce_dispute', 'user_id', FALSE, NULL),
        ('idx_commerce_dispute_resolved_by_id', 'commerce_dispute', 'resolved_by_id', FALSE, NULL),
        ('idx_commerce_flash_sale_created_by', 'commerce_flash_sale', 'created_by', FALSE, NULL),
        ('idx_commerce_handover_pass_order_id', 'commerce_handover_pass', 'order_id', FALSE, NULL),
        ('idx_commerce_handover_pass_verified_by_user_id', 'commerce_handover_pass', 'verified_by_user_id', FALSE, NULL),
        ('idx_commerce_inventory_product_id', 'commerce_inventory', 'product_id', FALSE, NULL),
        ('idx_commerce_order_seller_id', 'commerce_order', 'seller_id', FALSE, NULL),
        ('idx_commerce_order_customer_profile_id', 'commerce_order', 'customer_profile_id', FALSE, NULL),
        ('idx_commerce_order_invoice_document_id', 'commerce_order', 'invoice_document_id', FALSE, NULL),
        ('idx_commerce_payment_order_id', 'commerce_payment', 'order_id', FALSE, NULL),
        ('idx_commerce_product_category_parent_id', 'commerce_product_category', 'parent_id', FALSE, NULL),
        ('idx_commerce_return_resolved_by_id', 'commerce_return', 'resolved_by_id', FALSE, NULL),
        ('idx_commerce_return_exchange_order_id', 'commerce_return', 'exchange_order_id', FALSE, NULL),
        ('idx_commerce_review_order_id', 'commerce_review', 'order_id', FALSE, NULL),
        ('idx_commerce_review_user_id', 'commerce_review', 'user_id', FALSE, NULL),
        ('idx_commerce_settlement_seller_id', 'commerce_settlement', 'seller_id', FALSE, NULL),
        ('idx_commerce_subscription_product_id', 'commerce_subscription', 'product_id', FALSE, NULL),
        ('idx_commerce_subscription_seller_id', 'commerce_subscription', 'seller_id', FALSE, NULL),
        ('idx_commerce_wallet_community_id', 'commerce_wallet', 'community_id', FALSE, NULL),
        ('idx_committee_elections_position_id', 'committee_elections', 'position_id', FALSE, NULL),
        ('idx_community_pins_user_id', 'community_pins', 'user_id', FALSE, NULL),
        ('idx_community_saas_subscription_community_id', 'community_saas_subscription', 'community_id', FALSE, NULL),
        ('idx_community_security_record_actor_id', 'community_security_record', 'actor_id', FALSE, NULL),
        ('idx_community_wallet_credit_lines_wallet_id', 'community_wallet_credit_lines', 'wallet_id', FALSE, NULL),
        ('idx_commute_rating_rater_id', 'commute_rating', 'rater_id', FALSE, NULL),
        ('idx_commute_ride_vehicle_id', 'commute_ride', 'vehicle_id', FALSE, NULL),
        ('idx_content_report_resolved_by_id', 'content_report', 'resolved_by_id', FALSE, NULL),
        ('idx_cpos_crm_document_community_id', 'cpos_crm_document', 'community_id', FALSE, NULL),
        ('idx_cpos_crm_drip_sequence_community_id', 'cpos_crm_drip_sequence', 'community_id', FALSE, NULL),
        ('idx_cpos_crm_drip_step_sequence_id', 'cpos_crm_drip_step', 'sequence_id', FALSE, NULL),
        ('idx_cpos_crm_email_campaign_community_id', 'cpos_crm_email_campaign', 'community_id', FALSE, NULL),
        ('idx_cpos_crm_lead_tag_tag_id', 'cpos_crm_lead_tag', 'tag_id', FALSE, NULL),
        ('idx_delivery_slot_template_id', 'delivery_slot', 'template_id', FALSE, NULL),
        ('idx_document_download_log_version_id', 'document_download_log', 'version_id', FALSE, NULL),
        ('idx_election_votes_nomination_id', 'election_votes', 'nomination_id', FALSE, NULL),
        ('idx_event_auction_bid_event_id', 'event_auction_bid', 'event_id', FALSE, NULL),
        ('idx_event_pooja_seva_time_slots_pooja_seva_id', 'event_pooja_seva_time_slots', 'pooja_seva_id', FALSE, NULL),
        ('idx_food_grocery_products_vendor_variant_id', 'food_grocery_products', 'vendor_variant_id', FALSE, NULL),
        ('idx_fresh_batch_release_released_by_user_id', 'fresh_batch_release', 'released_by_user_id', FALSE, NULL),
        ('idx_fresh_batch_temperature_log_logged_by_user_id', 'fresh_batch_temperature_log', 'logged_by_user_id', FALSE, NULL),
        ('idx_fresh_delivery_batch_assigned_runner_id', 'fresh_delivery_batch', 'assigned_runner_id', FALSE, NULL),
        ('idx_fresh_delivery_item_commerce_order_id', 'fresh_delivery_item', 'commerce_order_id', FALSE, NULL),
        ('idx_fresh_fulfilment_item_batch_id', 'fresh_fulfilment_item', 'batch_id', FALSE, NULL),
        ('idx_fresh_fulfilment_location_zone_id', 'fresh_fulfilment_location', 'zone_id', FALSE, NULL),
        ('idx_fresh_processing_batch_recipe_id', 'fresh_processing_batch', 'recipe_id', FALSE, NULL),
        ('idx_fresh_processing_batch_vendor_product_id', 'fresh_processing_batch', 'vendor_product_id', FALSE, NULL),
        ('idx_fresh_processing_recipe_output_product_id', 'fresh_processing_recipe', 'output_product_id', FALSE, NULL),
        ('idx_fresh_product_pricing_created_by_vendor_id', 'fresh_product_pricing', 'created_by_vendor_id', FALSE, NULL),
        ('idx_fresh_product_pricing_admin_approved_by', 'fresh_product_pricing', 'admin_approved_by', FALSE, NULL),
        ('idx_fresh_product_recall_initiated_by_user_id', 'fresh_product_recall', 'initiated_by_user_id', FALSE, NULL),
        ('idx_fresh_product_recall_order_fulfilment_item_id', 'fresh_product_recall_order', 'fulfilment_item_id', FALSE, NULL),
        ('idx_fresh_product_wastage_recorded_by_user_id', 'fresh_product_wastage', 'recorded_by_user_id', FALSE, NULL),
        ('idx_fresh_purchase_order_item_vendor_product_id', 'fresh_purchase_order_item', 'vendor_product_id', FALSE, NULL),
        ('idx_fresh_stock_movement_created_by_user_id', 'fresh_stock_movement', 'created_by_user_id', FALSE, NULL),
        ('idx_fresh_vendor_capability_approved_by_user_id', 'fresh_vendor_capability', 'approved_by_user_id', FALSE, NULL),
        ('idx_fresh_vendor_document_verified_by_user_id', 'fresh_vendor_document', 'verified_by_user_id', FALSE, NULL),
        ('idx_fresh_vendor_staff_invited_by', 'fresh_vendor_staff', 'invited_by', FALSE, NULL),
        ('idx_fresh_weight_adjustment_log_weighed_by_user_id', 'fresh_weight_adjustment_log', 'weighed_by_user_id', FALSE, NULL),
        ('idx_governance_proposal_meeting_id', 'governance_proposal', 'meeting_id', FALSE, NULL),
        ('idx_governance_resolution_meeting_id', 'governance_resolution', 'meeting_id', FALSE, NULL),
        ('idx_group_buy_demand_created_by_user_id', 'group_buy_demand', 'created_by_user_id', FALSE, NULL),
        ('idx_group_deal_created_by_id', 'group_deal', 'created_by_id', FALSE, NULL),
        ('idx_group_deal_vendor_id', 'group_deal', 'vendor_id', FALSE, NULL),
        ('idx_group_deal_tier_deal_id', 'group_deal_tier', 'deal_id', FALSE, NULL),
        ('idx_group_demand_item_requested_by_id', 'group_demand_item', 'requested_by_id', FALSE, NULL),
        ('idx_group_demand_upvote_user_id', 'group_demand_upvote', 'user_id', FALSE, NULL),
        ('idx_group_expense_splits_expense_id', 'group_expense_splits', 'expense_id', FALSE, NULL),
        ('idx_group_expenses_group_id', 'group_expenses', 'group_id', FALSE, NULL),
        ('idx_group_order_community_id', 'group_order', 'community_id', FALSE, NULL),
        ('idx_group_order_updates_group_order_id', 'group_order_updates', 'group_order_id', FALSE, NULL),
        ('idx_health_data_access_log_community_id', 'health_data_access_log', 'community_id', FALSE, NULL),
        ('idx_home_service_booking_provider_id', 'home_service_booking', 'provider_id', FALSE, NULL),
        ('idx_hr_appraisal_forms_appraisal_cycle_id', 'hr_appraisal_forms', 'appraisal_cycle_id', FALSE, NULL),
        ('idx_hr_appraisal_forms_reviewer_id', 'hr_appraisal_forms', 'reviewer_id', FALSE, NULL),
        ('idx_hr_asset_assignments_asset_id', 'hr_asset_assignments', 'asset_id', FALSE, NULL),
        ('idx_hr_career_paths_mentor_employee_id', 'hr_career_paths', 'mentor_employee_id', FALSE, NULL),
        ('idx_hr_compliance_alerts_employee_id', 'hr_compliance_alerts', 'employee_id', FALSE, NULL),
        ('idx_hr_employee_shifts_shift_id', 'hr_employee_shifts', 'shift_id', FALSE, NULL),
        ('idx_hr_employees_branch_id', 'hr_employees', 'branch_id', FALSE, NULL),
        ('idx_hr_employees_department_id', 'hr_employees', 'department_id', FALSE, NULL),
        ('idx_hr_employees_designation_id', 'hr_employees', 'designation_id', FALSE, NULL),
        ('idx_hr_employees_reporting_manager_id', 'hr_employees', 'reporting_manager_id', FALSE, NULL),
        ('idx_hr_expense_items_expense_claim_id', 'hr_expense_items', 'expense_claim_id', FALSE, NULL),
        ('idx_hr_feedback_requests_appraisal_cycle_id', 'hr_feedback_requests', 'appraisal_cycle_id', FALSE, NULL),
        ('idx_hr_feedback_responses_reviewer_id', 'hr_feedback_responses', 'reviewer_id', FALSE, NULL),
        ('idx_hr_goals_appraisal_cycle_id', 'hr_goals', 'appraisal_cycle_id', FALSE, NULL),
        ('idx_hr_helpdesk_tickets_assigned_to_employee_id', 'hr_helpdesk_tickets', 'assigned_to_employee_id', FALSE, NULL),
        ('idx_hr_interviews_interviewer_employee_id', 'hr_interviews', 'interviewer_employee_id', FALSE, NULL),
        ('idx_hr_leave_balances_leave_type_id', 'hr_leave_balances', 'leave_type_id', FALSE, NULL),
        ('idx_hr_leave_requests_leave_type_id', 'hr_leave_requests', 'leave_type_id', FALSE, NULL),
        ('idx_hr_payslips_payroll_run_id', 'hr_payslips', 'payroll_run_id', FALSE, NULL),
        ('idx_hr_recognitions_giver_employee_id', 'hr_recognitions', 'giver_employee_id', FALSE, NULL),
        ('idx_hr_tasks_assigned_by_employee_id', 'hr_tasks', 'assigned_by_employee_id', FALSE, NULL),
        ('idx_hr_training_enrollments_training_program_id', 'hr_training_enrollments', 'training_program_id', FALSE, NULL),
        ('idx_hr_travel_requests_employee_id', 'hr_travel_requests', 'employee_id', FALSE, NULL),
        ('idx_hs_booking_package_id', 'hs_booking', 'package_id', FALSE, NULL),
        ('idx_hs_recurring_plan_package_id', 'hs_recurring_plan', 'package_id', FALSE, NULL),
        ('idx_hs_rework_request_booking_id', 'hs_rework_request', 'booking_id', FALSE, NULL),
        ('idx_hs_worker_payout_booking_id', 'hs_worker_payout', 'booking_id', FALSE, NULL),
        ('idx_insurance_application_quote_id', 'insurance_application', 'quote_id', FALSE, NULL),
        ('idx_insurance_application_user_id', 'insurance_application', 'user_id', FALSE, NULL),
        ('idx_insurance_application_partner_id', 'insurance_application', 'partner_id', FALSE, NULL),
        ('idx_insurance_claims_assistance_case_policy_id', 'insurance_claims_assistance_case', 'policy_id', FALSE, NULL),
        ('idx_insurance_consent_record_partner_id', 'insurance_consent_record', 'partner_id', FALSE, NULL),
        ('idx_insurance_group_enquiry_consent_id', 'insurance_group_enquiry', 'consent_id', FALSE, NULL),
        ('idx_insurance_partner_webhook_event_partner_id', 'insurance_partner_webhook_event', 'partner_id', FALSE, NULL),
        ('idx_insurance_policy_partner_id', 'insurance_policy', 'partner_id', FALSE, NULL),
        ('idx_insurance_policy_document_policy_id', 'insurance_policy_document', 'policy_id', FALSE, NULL),
        ('idx_insurance_policy_document_owner_user_id', 'insurance_policy_document', 'owner_user_id', FALSE, NULL),
        ('idx_insurance_quote_partner_id', 'insurance_quote', 'partner_id', FALSE, NULL),
        ('idx_insurance_quote_product_version_id', 'insurance_quote', 'product_version_id', FALSE, NULL),
        ('idx_insurance_quote_request_partner_id', 'insurance_quote_request', 'partner_id', FALSE, NULL),
        ('idx_insurance_quote_request_consent_id', 'insurance_quote_request', 'consent_id', FALSE, NULL),
        ('idx_insurance_renewal_event_policy_id', 'insurance_renewal_event', 'policy_id', FALSE, NULL),
        ('idx_insured_member_policy_id', 'insured_member', 'policy_id', FALSE, NULL),
        ('idx_interior_change_request_proposed_by', 'interior_change_request', 'proposed_by', FALSE, NULL),
        ('idx_interior_change_request_decided_by', 'interior_change_request', 'decided_by', FALSE, NULL),
        ('idx_interior_payment_event_milestone_id', 'interior_payment_event', 'milestone_id', FALSE, NULL),
        ('idx_interior_payment_event_actor_user_id', 'interior_payment_event', 'actor_user_id', FALSE, NULL),
        ('idx_interior_payment_milestone_provider_id', 'interior_payment_milestone', 'provider_id', FALSE, NULL),
        ('idx_interior_saved_designer_provider_id', 'interior_saved_designer', 'provider_id', FALSE, NULL),
        ('idx_interior_saved_inspiration_service_request_id', 'interior_saved_inspiration', 'service_request_id', FALSE, NULL),
        ('idx_inventory_stock_ledger_entry_location_id', 'inventory_stock_ledger_entry', 'location_id', FALSE, NULL),
        ('idx_inventory_stock_ledger_entry_lot_id', 'inventory_stock_ledger_entry', 'lot_id', FALSE, NULL),
        ('idx_inventory_stock_ledger_entry_created_by', 'inventory_stock_ledger_entry', 'created_by', FALSE, NULL),
        ('idx_jewellery_photo_job_product_id', 'jewellery_photo_job', 'product_id', FALSE, NULL),
        ('idx_jewellery_tryon_session_product_id', 'jewellery_tryon_session', 'product_id', FALSE, NULL),
        ('idx_jewellery_wishlist_product_id', 'jewellery_wishlist', 'product_id', FALSE, NULL),
        ('idx_karate_enrollment_current_belt_id', 'karate_enrollment', 'current_belt_id', FALSE, NULL),
        ('idx_karate_exam_target_belt_id', 'karate_exam', 'target_belt_id', FALSE, NULL),
        ('idx_karate_exam_result_awarded_belt_id', 'karate_exam_result', 'awarded_belt_id', FALSE, NULL),
        ('idx_karate_program_starting_belt_id', 'karate_program', 'starting_belt_id', FALSE, NULL),
        ('idx_karate_program_target_belt_id', 'karate_program', 'target_belt_id', FALSE, NULL),
        ('idx_kitty_circle_created_by', 'kitty_circle', 'created_by', FALSE, NULL),
        ('idx_kitty_circle_invitation_circle_id', 'kitty_circle_invitation', 'circle_id', FALSE, NULL),
        ('idx_kitty_circle_invitation_invited_user_id', 'kitty_circle_invitation', 'invited_user_id', FALSE, NULL),
        ('idx_kitty_circle_invitation_created_by', 'kitty_circle_invitation', 'created_by', FALSE, NULL),
        ('idx_kitty_circle_join_request_user_id', 'kitty_circle_join_request', 'user_id', FALSE, NULL),
        ('idx_kitty_circle_join_request_reviewed_by', 'kitty_circle_join_request', 'reviewed_by', FALSE, NULL),
        ('idx_kitty_circle_member_community_id', 'kitty_circle_member', 'community_id', FALSE, NULL),
        ('idx_kitty_circle_member_approved_by', 'kitty_circle_member', 'approved_by', FALSE, NULL),
        ('idx_kitty_contribution_member_user_id', 'kitty_contribution', 'member_user_id', FALSE, NULL),
        ('idx_kitty_contribution_verified_by', 'kitty_contribution', 'verified_by', FALSE, NULL),
        ('idx_kitty_cycle_circle_id', 'kitty_cycle', 'circle_id', FALSE, NULL),
        ('idx_kitty_cycle_created_by', 'kitty_cycle', 'created_by', FALSE, NULL),
        ('idx_kitty_dispute_raised_by', 'kitty_dispute', 'raised_by', FALSE, NULL),
        ('idx_kitty_dispute_resolved_by', 'kitty_dispute', 'resolved_by', FALSE, NULL),
        ('idx_kitty_event_created_by', 'kitty_event', 'created_by', FALSE, NULL),
        ('idx_kitty_event_attendance_user_id', 'kitty_event_attendance', 'user_id', FALSE, NULL),
        ('idx_kitty_event_attendance_marked_by', 'kitty_event_attendance', 'marked_by', FALSE, NULL),
        ('idx_kitty_expense_share_user_id', 'kitty_expense_share', 'user_id', FALSE, NULL),
        ('idx_kitty_financial_audit_actor_id', 'kitty_financial_audit', 'actor_id', FALSE, NULL),
        ('idx_kitty_game_player_user_id', 'kitty_game_player', 'user_id', FALSE, NULL),
        ('idx_kitty_game_room_circle_id', 'kitty_game_room', 'circle_id', FALSE, NULL),
        ('idx_kitty_game_room_created_by', 'kitty_game_room', 'created_by', FALSE, NULL),
        ('idx_kitty_game_ticket_player_id', 'kitty_game_ticket', 'player_id', FALSE, NULL),
        ('idx_kitty_game_winner_ticket_id', 'kitty_game_winner', 'ticket_id', FALSE, NULL),
        ('idx_kitty_game_winner_user_id', 'kitty_game_winner', 'user_id', FALSE, NULL),
        ('idx_kitty_rotation_beneficiary_user_id', 'kitty_rotation', 'beneficiary_user_id', FALSE, NULL),
        ('idx_kyc_document_reviewed_by', 'kyc_document', 'reviewed_by', FALSE, NULL),
        ('idx_market_application_stall_id', 'market_application', 'stall_id', FALSE, NULL),
        ('idx_market_booking_applicant_id', 'market_booking', 'applicant_id', FALSE, NULL),
        ('idx_match_photo_uploaded_by_id', 'match_photo', 'uploaded_by_id', FALSE, NULL),
        ('idx_match_photo_like_user_id', 'match_photo_like', 'user_id', FALSE, NULL),
        ('idx_match_player_rating_rater_id', 'match_player_rating', 'rater_id', FALSE, NULL),
        ('idx_match_player_rating_rated_player_id', 'match_player_rating', 'rated_player_id', FALSE, NULL),
        ('idx_meal_item_meal_log_id', 'meal_item', 'meal_log_id', FALSE, NULL),
        ('idx_meter_billing_cycle_summaries_community_id', 'meter_billing_cycle_summaries', 'community_id', FALSE, NULL),
        ('idx_mobility_carpool_offer_driver_user_id', 'mobility_carpool_offer', 'driver_user_id', FALSE, NULL),
        ('idx_mobility_driver_document_driver_id', 'mobility_driver_document', 'driver_id', FALSE, NULL),
        ('idx_mobility_driver_document_reviewed_by', 'mobility_driver_document', 'reviewed_by', FALSE, NULL),
        ('idx_mobility_driver_offer_trip_id', 'mobility_driver_offer', 'trip_id', FALSE, NULL),
        ('idx_mobility_journey_match_offer_id', 'mobility_journey_match', 'offer_id', FALSE, NULL),
        ('idx_mobility_journey_slot_created_by', 'mobility_journey_slot', 'created_by', FALSE, NULL),
        ('idx_mobility_service_zone_community_id', 'mobility_service_zone', 'community_id', FALSE, NULL),
        ('idx_mobility_trip_vehicle_id', 'mobility_trip', 'vehicle_id', FALSE, NULL),
        ('idx_mobility_vehicle_driver_id', 'mobility_vehicle', 'driver_id', FALSE, NULL),
        ('idx_mw_passport_certifications_passport_id', 'mw_passport_certifications', 'passport_id', FALSE, NULL),
        ('idx_mw_passport_skills_passport_id', 'mw_passport_skills', 'passport_id', FALSE, NULL),
        ('idx_mw_passport_work_history_passport_id', 'mw_passport_work_history', 'passport_id', FALSE, NULL),
        ('idx_parking_slot_booking_slot_id', 'parking_slot_booking', 'slot_id', FALSE, NULL),
        ('idx_parking_visitor_pass_spot_id', 'parking_visitor_pass', 'spot_id', FALSE, NULL),
        ('idx_payment_dispute_receipt_id', 'payment_dispute', 'receipt_id', FALSE, NULL),
        ('idx_post_comment_parent_id', 'post_comment', 'parent_id', FALSE, NULL),
        ('idx_post_hashtag_hashtag_id', 'post_hashtag', 'hashtag_id', FALSE, NULL),
        ('idx_recurring_basket_slot_template_id', 'recurring_basket', 'slot_template_id', FALSE, NULL),
        ('idx_recurring_basket_item_basket_id', 'recurring_basket_item', 'basket_id', FALSE, NULL),
        ('idx_school_bus_community_id', 'school_bus', 'community_id', FALSE, NULL),
        ('idx_school_bus_registered_child_bus_id', 'school_bus_registered_child', 'bus_id', FALSE, NULL),
        ('idx_sensor_alert_configs_sensor_id', 'sensor_alert_configs', 'sensor_id', FALSE, NULL),
        ('idx_sensor_anomaly_events_sensor_id', 'sensor_anomaly_events', 'sensor_id', FALSE, NULL),
        ('idx_sensor_anomaly_events_alert_config_id', 'sensor_anomaly_events', 'alert_config_id', FALSE, NULL),
        ('idx_service_provider_credential_evidence_media_id', 'service_provider_credential', 'evidence_media_id', FALSE, NULL),
        ('idx_service_provider_credential_submitted_by', 'service_provider_credential', 'submitted_by', FALSE, NULL),
        ('idx_service_provider_credential_reviewed_by', 'service_provider_credential', 'reviewed_by', FALSE, NULL),
        ('idx_service_provider_credential_audit_credential_id', 'service_provider_credential_audit', 'credential_id', FALSE, NULL),
        ('idx_service_provider_credential_audit_actor_user_id', 'service_provider_credential_audit', 'actor_user_id', FALSE, NULL),
        ('idx_sms_message_template_id', 'sms_message', 'template_id', FALSE, NULL),
        ('idx_sports_auction_config_updated_by_user_id', 'sports_auction_config', 'updated_by_user_id', FALSE, NULL),
        ('idx_sports_batch_coach_id', 'sports_batch', 'coach_id', FALSE, NULL),
        ('idx_sports_event_registration_reviewed_by_user_id', 'sports_event_registration', 'reviewed_by_user_id', FALSE, NULL),
        ('idx_sports_event_registration_category_id', 'sports_event_registration', 'category_id', FALSE, NULL),
        ('idx_sports_external_form_created_by_user_id', 'sports_external_form', 'created_by_user_id', FALSE, NULL),
        ('idx_sports_external_response_linked_registration_id', 'sports_external_response', 'linked_registration_id', FALSE, NULL),
        ('idx_sports_external_response_resolved_user_id', 'sports_external_response', 'resolved_user_id', FALSE, NULL),
        ('idx_sports_external_response_reviewed_by_user_id', 'sports_external_response', 'reviewed_by_user_id', FALSE, NULL),
        ('idx_sports_match_official_user_id', 'sports_match_official', 'user_id', FALSE, NULL),
        ('idx_sports_player_match_stats_team_id', 'sports_player_match_stats', 'team_id', FALSE, NULL),
        ('idx_sports_race_result_team_id', 'sports_race_result', 'team_id', FALSE, NULL),
        ('idx_sports_stage_publication_version_id', 'sports_stage_publication', 'version_id', FALSE, NULL),
        ('idx_sports_tournament_created_by_user_id', 'sports_tournament', 'created_by_user_id', FALSE, NULL),
        ('idx_sports_tournament_config_updated_by_user_id', 'sports_tournament_config', 'updated_by_user_id', FALSE, NULL),
        ('idx_sports_tournament_match_schedule_version_id', 'sports_tournament_match', 'schedule_version_id', FALSE, NULL),
        ('idx_sports_tournament_match_postponed_from_match_id', 'sports_tournament_match', 'postponed_from_match_id', FALSE, NULL),
        ('idx_sports_tournament_player_payment_user_id', 'sports_tournament_player_payment', 'user_id', FALSE, NULL),
        ('idx_sports_tournament_player_payment_family_member_id', 'sports_tournament_player_payment', 'family_member_id', FALSE, NULL),
        ('idx_sports_tournament_player_payment_marked_by_user_id', 'sports_tournament_player_payment', 'marked_by_user_id', FALSE, NULL),
        ('idx_sports_tournament_player_payment_payment_order_id', 'sports_tournament_player_payment', 'payment_order_id', FALSE, NULL),
        ('idx_sports_tournament_reserve_day_stage_id', 'sports_tournament_reserve_day', 'stage_id', FALSE, NULL),
        ('idx_sports_tournament_reserve_day_venue_id', 'sports_tournament_reserve_day', 'venue_id', FALSE, NULL),
        ('idx_sports_tournament_reserve_day_court_id', 'sports_tournament_reserve_day', 'court_id', FALSE, NULL),
        ('idx_sports_vendor_booking_court_id', 'sports_vendor_booking', 'court_id', FALSE, NULL),
        ('idx_sports_vendor_booking_slot_id', 'sports_vendor_booking', 'slot_id', FALSE, NULL),
        ('idx_sports_workout_link_workout_log_id', 'sports_workout_link', 'workout_log_id', FALSE, NULL),
        ('idx_step_log_device_id', 'step_log', 'device_id', FALSE, NULL),
        ('idx_user_program_enrollment_program_id', 'user_program_enrollment', 'program_id', FALSE, NULL),
        ('idx_vehicle_booking_addon_addon_id', 'vehicle_booking_addon', 'addon_id', FALSE, NULL),
        ('idx_vehicle_care_booking_vehicle_id', 'vehicle_care_booking', 'vehicle_id', FALSE, NULL),
        ('idx_vehicle_care_booking_service_id', 'vehicle_care_booking', 'service_id', FALSE, NULL),
        ('idx_vehicle_care_booking_technician_id', 'vehicle_care_booking', 'technician_id', FALSE, NULL),
        ('idx_vehicle_care_plan_plan_catalog_id', 'vehicle_care_plan', 'plan_catalog_id', FALSE, NULL),
        ('idx_vehicle_care_plan_vehicle_id', 'vehicle_care_plan', 'vehicle_id', FALSE, NULL),
        ('idx_vehicle_profile_flat_id', 'vehicle_profile', 'flat_id', FALSE, NULL),
        ('idx_vehicle_service_slot_service_id', 'vehicle_service_slot', 'service_id', FALSE, NULL),
        ('idx_vendor_dpa_community_id', 'vendor_dpa', 'community_id', FALSE, NULL),
        ('idx_vendor_product_vms_vendor_id', 'vendor_product', 'vms_vendor_id', FALSE, NULL),
        ('idx_vendor_product_commerce_category_id', 'vendor_product', 'commerce_category_id', FALSE, NULL),
        ('idx_vms_vendor_registrations_applicant_user_id', 'vms_vendor_registrations', 'applicant_user_id', FALSE, NULL),
        ('idx_vms_vendor_registrations_reviewed_by', 'vms_vendor_registrations', 'reviewed_by', FALSE, NULL),
        ('idx_water_tanker_deliveries_target_tank_id', 'water_tanker_deliveries', 'target_tank_id', FALSE, NULL),
        ('idx_wellness_menu_swap_original_product_id', 'wellness_menu_swap', 'original_product_id', FALSE, NULL),
        ('idx_wellness_menu_swap_requested_product_id', 'wellness_menu_swap', 'requested_product_id', FALSE, NULL),
        ('idx_workout_log_exercise_catalog_id', 'workout_log', 'exercise_catalog_id', FALSE, NULL)
    ) AS t(idx_name, tbl_name, col_list, is_unique, where_clause)
    LOOP
        -- Skip if table does not exist in manacommunity schema
        IF to_regclass('manacommunity.' || rec.tbl_name) IS NULL THEN
            CONTINUE;
        END IF;

        -- Verify every column referenced by the index exists
        col_arr := string_to_array(rec.col_list, ',');
        all_cols_exist := TRUE;
        FOREACH c IN ARRAY col_arr LOOP
            col_only := split_part(trim(c), ' ', 1);
            IF NOT EXISTS (
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'manacommunity'
                  AND table_name = rec.tbl_name
                  AND column_name = col_only
            ) THEN
                all_cols_exist := FALSE;
                EXIT;
            END IF;
        END LOOP;
        IF NOT all_cols_exist THEN
            CONTINUE;
        END IF;

        -- Skip if exact index name already exists
        IF to_regclass('manacommunity.' || rec.idx_name) IS NOT NULL THEN
            CONTINUE;
        END IF;

        -- For standard non-unique unconditional single-column indexes, skip if an existing
        -- non-partial index already covers that exact column as its leading key.
        already_covered := FALSE;
        IF NOT rec.is_unique AND rec.where_clause IS NULL AND array_length(col_arr, 1) = 1 THEN
            col_only := split_part(trim(col_arr[1]), ' ', 1);
            SELECT EXISTS (
                SELECT 1
                FROM pg_index ix
                JOIN pg_class tc ON tc.oid = ix.indrelid
                JOIN pg_namespace ns ON ns.oid = tc.relnamespace
                JOIN pg_attribute a ON a.attrelid = tc.oid AND a.attnum = (ix.indkey::int2[])[1]
                WHERE ns.nspname = 'manacommunity'
                  AND tc.relname = rec.tbl_name
                  AND ix.indisvalid
                  AND ix.indpred IS NULL
                  AND a.attname = col_only
            ) INTO already_covered;
        END IF;
        IF already_covered THEN
            CONTINUE;
        END IF;

        -- Guard UNIQUE index creation if legacy data happens to contain duplicates; downgrade to non-unique lookup index rather than failing migration
        IF rec.is_unique THEN
            DECLARE
                has_dupes BOOLEAN := FALSE;
                first_col TEXT := split_part(trim(col_arr[1]), ' ', 1);
            BEGIN
                EXECUTE format(
                    'SELECT EXISTS (SELECT 1 FROM manacommunity.%I WHERE ' || COALESCE(rec.where_clause, first_col || ' IS NOT NULL') || ' GROUP BY ' || rec.col_list || ' HAVING COUNT(*) > 1)',
                    rec.tbl_name
                ) INTO has_dupes;
                IF has_dupes THEN
                    stmt := 'CREATE INDEX IF NOT EXISTS ' || quote_ident(rec.idx_name)
                         || ' ON manacommunity.' || quote_ident(rec.tbl_name)
                         || ' (' || rec.col_list || ')'
                         || CASE WHEN rec.where_clause IS NOT NULL THEN ' WHERE ' || rec.where_clause ELSE '' END;
                    EXECUTE stmt;
                    CONTINUE;
                END IF;
            END;
        END IF;

        stmt := 'CREATE ' || CASE WHEN rec.is_unique THEN 'UNIQUE ' ELSE '' END
             || 'INDEX IF NOT EXISTS ' || quote_ident(rec.idx_name)
             || ' ON manacommunity.' || quote_ident(rec.tbl_name)
             || ' (' || rec.col_list || ')'
             || CASE WHEN rec.where_clause IS NOT NULL THEN ' WHERE ' || rec.where_clause ELSE '' END;
        EXECUTE stmt;
    END LOOP;
END $$;

-- ── 2. Drop Verified Redundant Indexes ────────────────────────────────────
DO $$
DECLARE
    rec RECORD;
    has_cover BOOLEAN;
    is_constraint_owned BOOLEAN;
BEGIN
    FOR rec IN SELECT * FROM (VALUES
        ('idx_ambulance_booking_user_id', 'ambulance_booking', 'idx_ambulance_booking_user_status', NULL),
        ('idx_body_metric_user_date', 'body_metric_log', 'idx_body_metric_user_date_unique', NULL),
        ('idx_cp_community', 'coach_package', 'idx_cp_comm', NULL),
        ('idx_comm_order_buyer', 'commerce_order', 'idx_co_buyer_status', NULL),
        ('idx_comm_order_comm', 'commerce_order', 'idx_co_community_status', NULL),
        ('idx_court_sub_court', 'court_slot_subscription', 'idx_css_court', NULL),
        ('idx_waitlist_community', 'court_slot_waitlist', 'idx_csw_community', NULL),
        ('idx_finance_invoice_community', 'finance_invoice', 'idx_finance_invoice_community_status', NULL),
        ('idx_flat_membership_flat', 'flat_membership', 'idx_flat_membership_primary', NULL),
        ('idx_forum_post_community', 'forum_posts', 'idx_forum_post_category', NULL),
        ('idx_fresh_allergen_product', 'fresh_product_allergen', 'uq_fresh_allergen', NULL),
        ('idx_fresh_vendor_doc_vendor', 'fresh_vendor_document', 'idx_fresh_vendor_doc_type', NULL),
        ('idx_fresh_weight_adj_customer', 'fresh_weight_adjustment_log', 'idx_fresh_weight_adj_pending', NULL),
        ('idx_group_buy_deal_status', 'group_buy_deal', 'idx_group_buy_deal_lifecycle', NULL),
        ('idx_group_buy_order_status', 'group_buy_order', 'idx_group_buy_order_reserved', NULL),
        ('idx_guard_profile_community', 'guard_profile', 'idx_guard_profile_status', NULL),
        ('idx_guard_shift_community', 'guard_shift', 'idx_guard_shift_date', NULL),
        ('idx_home_svc_booking_community', 'home_service_booking', 'idx_home_svc_booking_resident', NULL),
        ('idx_staff_link_flat', 'household_staff_link', 'idx_staff_link_active', NULL),
        ('idx_hr_goals_employee', 'hr_goals', 'idx_hr_goals_cycle', NULL),
        ('idx_hr_job_postings_org', 'hr_job_postings', 'idx_hr_job_postings_status', NULL),
        ('idx_mana_vendor_community', 'mana_vendor', 'idx_mana_vendor_name', NULL),
        ('idx_mkt_seller_comm', 'market_seller_profiles', 'idx_msp_category', NULL),
        ('idx_notif_pref_user', 'notification_preferences', 'idx_notif_pref_lookup', NULL),
        ('idx_opp_session', 'open_play_participant', 'idx_opp_sess', NULL),
        ('idx_ops_community', 'open_play_session', 'idx_ops_comm', NULL),
        ('idx_payment_order_community', 'payment_order', 'idx_payment_order_community_status', NULL),
        ('idx_pr_rated_user', 'player_rating', 'idx_pr_rated', NULL),
        ('idx_post_community', 'post', 'idx_post_type', NULL),
        ('idx_pna_resident', 'privacy_notice_acceptances', 'idx_pna_resident_version', NULL),
        ('idx_sports_event_community_id', 'sports_event', 'idx_event_community_status', NULL),
        ('idx_reg_community_status', 'sports_event_registration', 'idx_sports_event_reg_comm', NULL),
        ('idx_ball_event_match_id', 'sports_match_ball_event', 'idx_ball_event_match_over', NULL),
        ('idx_deposit_user', 'sports_no_show_deposit', 'idx_snsd_user', NULL),
        ('idx_player_stats_match_id', 'sports_player_match_stats', 'idx_player_match_stats_match', NULL),
        ('idx_player_stats_player_id', 'sports_player_match_stats', 'idx_player_match_stats_player', NULL),
        ('idx_race_result_match', 'sports_race_result', 'idx_race_result_heat', NULL),
        ('idx_race_result_player', 'sports_race_result', 'idx_race_result_player_id', NULL),
        ('idx_race_result_match_id', 'sports_race_result', 'idx_race_result_heat', NULL),
        ('idx_tournament_match_config_id', 'sports_tournament_match', 'idx_tournament_match_config_round', NULL),
        ('idx_tournament_match_community', 'sports_tournament_match', 'idx_sports_match_comm_sched', NULL),
        ('idx_loc_consent_user', 'user_location_consent', 'idx_loc_consent_purpose', NULL),
        ('idx_workout_log_user_id', 'workout_log', 'idx_workout_log_user_date', NULL),
        ('idx_cf_order_mana_order_id', 'cashfree_order', NULL, 'mana_order_id'),
        ('idx_sms_msg_reference', 'sms_message', NULL, 'message_reference'),
        ('idx_bulk_campaign_reference', 'bulk_sms_campaign', NULL, 'campaign_reference')
    ) AS d(drop_idx, tbl_name, cov_idx, uq_col)
    LOOP
        IF to_regclass('manacommunity.' || rec.drop_idx) IS NULL THEN
            CONTINUE;
        END IF;

        -- Never drop an index that backs a pg_constraint directly
        SELECT EXISTS (
            SELECT 1
            FROM pg_constraint c
            JOIN pg_class ic ON ic.oid = c.conindid
            JOIN pg_namespace ns ON ns.oid = ic.relnamespace
            WHERE ns.nspname = 'manacommunity' AND ic.relname = rec.drop_idx
        ) INTO is_constraint_owned;
        IF is_constraint_owned THEN
            CONTINUE;
        END IF;

        -- Verify covering index (or matching-column UNIQUE constraint when cov_idx is NULL) exists
        IF rec.cov_idx IS NOT NULL THEN
            has_cover := (to_regclass('manacommunity.' || rec.cov_idx) IS NOT NULL)
                      OR EXISTS (
                            SELECT 1 FROM pg_constraint c
                            JOIN pg_namespace ns ON ns.oid = c.connamespace
                            WHERE ns.nspname = 'manacommunity' AND c.conname = rec.cov_idx
                         );
        ELSE
            SELECT EXISTS (
                SELECT 1 FROM pg_constraint c
                JOIN pg_class tc ON tc.oid = c.conrelid
                JOIN pg_namespace ns ON ns.oid = tc.relnamespace
                JOIN pg_attribute a ON a.attrelid = tc.oid AND a.attnum = c.conkey[1]
                WHERE ns.nspname = 'manacommunity' AND tc.relname = rec.tbl_name
                  AND c.contype = 'u' AND array_length(c.conkey, 1) = 1 AND a.attname = rec.uq_col
            ) INTO has_cover;
        END IF;

        IF has_cover THEN
            EXECUTE 'DROP INDEX IF EXISTS manacommunity.' || quote_ident(rec.drop_idx);
        END IF;
    END LOOP;
END $$;

-- ── 3. Fix V68 vs V334 NOT NULL & UNIQUE Conflicts on SMS Tables ───────────
DO $$
DECLARE
    col TEXT;
    uq_rec RECORD;
BEGIN
    -- SmsMessage entity writes rendered_body / provider_name and omits legacy V68 NOT NULL columns
    IF to_regclass('manacommunity.sms_message') IS NOT NULL THEN
        FOREACH col IN ARRAY ARRAY['message_reference', 'masked_phone_number', 'message_body'] LOOP
            IF EXISTS (
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'manacommunity' AND table_name = 'sms_message'
                  AND column_name = col AND is_nullable = 'NO'
            ) THEN
                EXECUTE 'ALTER TABLE manacommunity.sms_message ALTER COLUMN ' || quote_ident(col) || ' DROP NOT NULL';
            END IF;
        END LOOP;
    END IF;

    -- UserSmsPreference entity uses composite UNIQUE (user_id, notification_type); drop legacy 1-col UNIQUE (user_id)
    IF to_regclass('manacommunity.user_sms_preference') IS NOT NULL THEN
        FOR uq_rec IN
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_class tc ON tc.oid = c.conrelid
            JOIN pg_namespace ns ON ns.oid = tc.relnamespace
            WHERE ns.nspname = 'manacommunity'
              AND tc.relname = 'user_sms_preference'
              AND c.contype = 'u'
              AND array_length(c.conkey, 1) = 1
              AND EXISTS (
                  SELECT 1 FROM pg_attribute a
                  WHERE a.attrelid = tc.oid AND a.attnum = c.conkey[1] AND a.attname = 'user_id'
              )
        LOOP
            EXECUTE 'ALTER TABLE manacommunity.user_sms_preference DROP CONSTRAINT IF EXISTS ' || quote_ident(uq_rec.conname);
        END LOOP;

        -- Ensure partial unique index on (user_id, notification_type) when notification_type is populated
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'manacommunity' AND table_name = 'user_sms_preference' AND column_name = 'notification_type'
        ) AND to_regclass('manacommunity.uk_user_sms_pref_user_type') IS NULL THEN
            DECLARE
                has_pref_dupes BOOLEAN := FALSE;
            BEGIN
                EXECUTE 'SELECT EXISTS (SELECT 1 FROM manacommunity.user_sms_preference WHERE notification_type IS NOT NULL GROUP BY user_id, notification_type HAVING COUNT(*) > 1)' INTO has_pref_dupes;
                IF NOT has_pref_dupes THEN
                    EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS uk_user_sms_pref_user_type ON manacommunity.user_sms_preference (user_id, notification_type) WHERE notification_type IS NOT NULL';
                END IF;
            END;
        END IF;
    END IF;

    -- Relax NOT NULL on dual-entity shared tables where newer entities omit legacy fields
    IF to_regclass('manacommunity.school_bus_trip_log') IS NOT NULL THEN
        FOREACH col IN ARRAY ARRAY['direction', 'bus_id'] LOOP
            IF EXISTS (
                SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'manacommunity' AND table_name = 'school_bus_trip_log'
                  AND column_name = col AND is_nullable = 'NO'
            ) THEN
                EXECUTE 'ALTER TABLE manacommunity.school_bus_trip_log ALTER COLUMN ' || quote_ident(col) || ' DROP NOT NULL';
            END IF;
        END LOOP;
    END IF;

    IF to_regclass('manacommunity.smart_meters') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'manacommunity' AND table_name = 'smart_meters'
              AND column_name = 'meter_serial_number' AND is_nullable = 'NO'
        ) THEN
            EXECUTE 'ALTER TABLE manacommunity.smart_meters ALTER COLUMN meter_serial_number DROP NOT NULL';
        END IF;
    END IF;
END $$;

-- ── 4. Create Upcoming Monthly Partitions for sensor_telemetry (V480) ──────
DO $$
DECLARE
    p RECORD;
    default_has_rows BOOLEAN;
BEGIN
    IF to_regclass('manacommunity.sensor_telemetry') IS NOT NULL THEN
        FOR p IN SELECT * FROM (VALUES
            ('sensor_telemetry_2026_12', '2026-12-01', '2027-01-01'),
            ('sensor_telemetry_2027_01', '2027-01-01', '2027-02-01'),
            ('sensor_telemetry_2027_02', '2027-02-01', '2027-03-01'),
            ('sensor_telemetry_2027_03', '2027-03-01', '2027-04-01'),
            ('sensor_telemetry_2027_04', '2027-04-01', '2027-05-01'),
            ('sensor_telemetry_2027_05', '2027-05-01', '2027-06-01'),
            ('sensor_telemetry_2027_06', '2027-06-01', '2027-07-01')
        ) AS m(part_name, start_dt, end_dt)
        LOOP
            IF to_regclass('manacommunity.' || p.part_name) IS NULL THEN
                default_has_rows := FALSE;
                IF to_regclass('manacommunity.sensor_telemetry_default') IS NOT NULL THEN
                    EXECUTE format(
                        'SELECT EXISTS (SELECT 1 FROM manacommunity.sensor_telemetry_default WHERE reading_at >= %L::timestamptz AND reading_at < %L::timestamptz)',
                        p.start_dt, p.end_dt
                    ) INTO default_has_rows;
                END IF;
                IF NOT default_has_rows THEN
                    EXECUTE format(
                        'CREATE TABLE IF NOT EXISTS manacommunity.%I PARTITION OF manacommunity.sensor_telemetry FOR VALUES FROM (%L) TO (%L)',
                        p.part_name, p.start_dt, p.end_dt
                    );
                END IF;
            END IF;
        END LOOP;
    END IF;
END $$;
