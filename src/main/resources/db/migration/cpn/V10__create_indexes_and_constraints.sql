-- Foreign Key Constraints (Selected prominent ones)
ALTER TABLE users ADD CONSTRAINT fk_user_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE user_sessions ADD CONSTRAINT fk_us_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE profiles ADD CONSTRAINT fk_prof_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE profiles ADD CONSTRAINT fk_prof_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE posts ADD CONSTRAINT fk_post_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Unique Constraints
ALTER TABLE users ADD CONSTRAINT uk_user_email_tenant UNIQUE (email, tenant_id);
ALTER TABLE companies ADD CONSTRAINT uk_company_slug UNIQUE (company_slug, tenant_id);
ALTER TABLE jobs ADD CONSTRAINT uk_job_slug UNIQUE (slug, tenant_id);
ALTER TABLE events ADD CONSTRAINT uk_event_slug UNIQUE (slug, tenant_id);

-- Check Constraints
ALTER TABLE jobs ADD CONSTRAINT chk_job_salary CHECK (salary_min <= salary_max);
ALTER TABLE jobs ADD CONSTRAINT chk_job_experience CHECK (experience_min <= experience_max);
ALTER TABLE company_reviews ADD CONSTRAINT chk_comp_review_rating CHECK (rating BETWEEN 1 AND 5);
ALTER TABLE mentorship_feedback ADD CONSTRAINT chk_ment_feedback_rating CHECK (rating BETWEEN 1 AND 5);

-- Common Composite Indexes
CREATE INDEX idx_jobs_tenant_status ON jobs(tenant_id, status);
CREATE INDEX idx_posts_tenant_status ON posts(tenant_id, is_published, deleted_at);
CREATE INDEX idx_events_tenant_status ON events(tenant_id, status);
CREATE INDEX idx_profiles_tenant_user ON profiles(tenant_id, user_id);

CREATE INDEX idx_post_reactions_pu ON post_reactions(post_id, user_id);
CREATE INDEX idx_bookmarks_pu ON bookmarks(post_id, user_id);
CREATE INDEX idx_jobs_company_status ON jobs(company_id, status);
CREATE INDEX idx_mentor_sessions_ms ON mentorship_sessions(mentor_id, status);

-- Partial Indexes
CREATE INDEX idx_users_active ON users(id) WHERE deleted_at IS NULL AND is_active = true;
CREATE INDEX idx_posts_active ON posts(id) WHERE deleted_at IS NULL AND is_published = true;
