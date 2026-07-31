CREATE TABLE freelance_projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    client_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    skills_required_json JSONB DEFAULT '[]'::jsonb,
    budget_type VARCHAR(50),
    budget_min NUMERIC(15,2),
    budget_max NUMERIC(15,2),
    currency VARCHAR(10),
    duration_days INT,
    status VARCHAR(50) DEFAULT 'OPEN',
    visibility VARCHAR(50) DEFAULT 'PUBLIC',
    proposal_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE freelance_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    freelancer_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    cover_letter TEXT,
    bid_amount NUMERIC(15,2),
    currency VARCHAR(10),
    estimated_days INT,
    status VARCHAR(50) DEFAULT 'PENDING',
    milestones_json JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE freelance_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    proposal_id UUID NOT NULL,
    client_id UUID NOT NULL,
    freelancer_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    total_amount NUMERIC(15,2),
    currency VARCHAR(10),
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    terms TEXT,
    signed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE freelance_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    amount NUMERIC(15,2),
    due_date DATE,
    status VARCHAR(50) DEFAULT 'PENDING',
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE freelance_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    milestone_id UUID,
    tenant_id UUID NOT NULL,
    payer_id UUID NOT NULL,
    payee_id UUID NOT NULL,
    amount NUMERIC(15,2),
    currency VARCHAR(10),
    payment_method VARCHAR(50),
    payment_status VARCHAR(50),
    transaction_id VARCHAR(255),
    escrow_released_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE freelance_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    reviewed_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    rating NUMERIC(3,2) NOT NULL,
    review_text TEXT,
    is_client_review BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mentor_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    expertise_areas_json JSONB DEFAULT '[]'::jsonb,
    mentoring_style TEXT,
    session_types_json JSONB DEFAULT '[]'::jsonb,
    languages_json JSONB DEFAULT '[]'::jsonb,
    hourly_rate NUMERIC(10,2),
    currency VARCHAR(10),
    is_free BOOLEAN DEFAULT false,
    free_sessions_per_month INT DEFAULT 0,
    total_sessions INT DEFAULT 0,
    average_rating NUMERIC(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    availability_json JSONB DEFAULT '{}'::jsonb,
    calendly_url TEXT,
    is_accepting_mentees BOOLEAN DEFAULT true,
    max_mentees INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mentorship_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    mentor_id UUID NOT NULL,
    mentee_id UUID NOT NULL,
    session_type VARCHAR(50),
    duration_minutes INT,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    meeting_link TEXT,
    notes TEXT,
    amount NUMERIC(10,2),
    currency VARCHAR(10),
    is_paid BOOLEAN DEFAULT false,
    payment_status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mentorship_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    reviewed_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    rating NUMERIC(3,2) NOT NULL,
    feedback_text TEXT,
    is_mentor_feedback BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mentorship_goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mentee_id UUID NOT NULL,
    mentor_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    goal_title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
