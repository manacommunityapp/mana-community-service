CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_slug VARCHAR(255) NOT NULL UNIQUE,
    logo_url TEXT,
    cover_url TEXT,
    industry VARCHAR(100),
    company_size VARCHAR(50),
    founded_year INT,
    website TEXT,
    description TEXT,
    culture_description TEXT,
    headquarters_location VARCHAR(255),
    is_verified BOOLEAN DEFAULT false,
    company_type VARCHAR(50),
    tech_stack_json JSONB DEFAULT '[]'::jsonb,
    social_links_json JSONB DEFAULT '{}'::jsonb,
    is_hiring BOOLEAN DEFAULT true,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE company_offices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    office_type VARCHAR(50),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE company_benefits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    benefit_name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50)
);

CREATE TABLE company_photos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    photo_url TEXT NOT NULL,
    caption VARCHAR(255),
    display_order INT DEFAULT 0
);

CREATE TABLE company_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    rating NUMERIC(3,2) NOT NULL,
    title VARCHAR(255),
    pros TEXT,
    cons TEXT,
    overall_rating NUMERIC(3,2),
    culture_rating NUMERIC(3,2),
    work_life_balance NUMERIC(3,2),
    career_growth NUMERIC(3,2),
    salary_benefits NUMERIC(3,2),
    management NUMERIC(3,2),
    is_current_employee BOOLEAN,
    employment_period VARCHAR(100),
    is_anonymous BOOLEAN DEFAULT false,
    status VARCHAR(50) DEFAULT 'APPROVED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    company_id UUID NOT NULL,
    posted_by UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    responsibilities TEXT,
    requirements TEXT,
    nice_to_have TEXT,
    job_type VARCHAR(50),
    work_mode VARCHAR(50),
    experience_min INT,
    experience_max INT,
    salary_min NUMERIC(15,2),
    salary_max NUMERIC(15,2),
    currency VARCHAR(10),
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    skills_required_json JSONB DEFAULT '[]'::jsonb,
    industry VARCHAR(100),
    department VARCHAR(100),
    vacancies INT DEFAULT 1,
    application_deadline TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    is_community_job BOOLEAN DEFAULT false,
    is_campus_job BOOLEAN DEFAULT false,
    is_featured BOOLEAN DEFAULT false,
    featured_until TIMESTAMP WITH TIME ZONE,
    view_count INT DEFAULT 0,
    application_count INT DEFAULT 0,
    is_easy_apply BOOLEAN DEFAULT false,
    external_apply_url TEXT,
    ats_job_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE job_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    applicant_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    resume_id UUID,
    cover_letter TEXT,
    status VARCHAR(50) DEFAULT 'APPLIED',
    applied_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_status_update TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    ai_match_score NUMERIC(5,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE application_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL,
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    changed_by UUID NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saved_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    job_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, job_id)
);

CREATE TABLE job_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    alert_name VARCHAR(255) NOT NULL,
    keywords TEXT,
    job_type VARCHAR(50),
    work_mode VARCHAR(50),
    location VARCHAR(255),
    experience_min INT,
    experience_max INT,
    salary_min NUMERIC(15,2),
    skills_json JSONB DEFAULT '[]'::jsonb,
    frequency VARCHAR(50) DEFAULT 'DAILY',
    is_active BOOLEAN DEFAULT true,
    last_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE referral_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    referrer_id UUID NOT NULL,
    company_id UUID NOT NULL,
    referral_bonus NUMERIC(15,2),
    terms TEXT,
    expiry_date TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT true,
    view_count INT DEFAULT 0,
    referral_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
