CREATE TABLE profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    headline VARCHAR(255),
    summary TEXT,
    profile_image_url TEXT,
    cover_image_url TEXT,
    current_company VARCHAR(255),
    current_designation VARCHAR(255),
    industry VARCHAR(100),
    location_city VARCHAR(100),
    location_state VARCHAR(100),
    location_country VARCHAR(100),
    experience_years NUMERIC(5,2),
    is_open_to_work BOOLEAN DEFAULT false,
    is_open_to_freelancing BOOLEAN DEFAULT false,
    is_open_to_mentoring BOOLEAN DEFAULT false,
    is_open_to_business BOOLEAN DEFAULT false,
    availability_type VARCHAR(50),
    availability_hours_per_week INT,
    linkedin_url TEXT,
    github_url TEXT,
    behance_url TEXT,
    dribbble_url TEXT,
    stackoverflow_url TEXT,
    portfolio_url TEXT,
    video_intro_url TEXT,
    profile_completion_pct INT DEFAULT 0,
    ai_skill_score INT DEFAULT 0,
    is_community_verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP WITH TIME ZONE,
    profile_views INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    skill_name VARCHAR(100) NOT NULL UNIQUE,
    skill_category VARCHAR(100),
    is_approved BOOLEAN DEFAULT true
);

CREATE TABLE profile_skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    skill_id UUID NOT NULL,
    proficiency_level VARCHAR(50),
    years_of_experience NUMERIC(5,2),
    is_primary BOOLEAN DEFAULT false,
    endorsement_count INT DEFAULT 0
);

CREATE TABLE skill_endorsements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_skill_id UUID NOT NULL,
    endorsed_by_profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE work_experience (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_id UUID,
    designation VARCHAR(255) NOT NULL,
    employment_type VARCHAR(50),
    start_date DATE NOT NULL,
    end_date DATE,
    is_current BOOLEAN DEFAULT false,
    location VARCHAR(255),
    description TEXT,
    technologies_used JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE education (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    degree VARCHAR(100),
    field_of_study VARCHAR(100),
    start_year INT,
    end_year INT,
    grade VARCHAR(50),
    activities TEXT,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE certifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    certification_name VARCHAR(255) NOT NULL,
    issuing_org VARCHAR(255) NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    credential_id VARCHAR(100),
    credential_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE languages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    language_name VARCHAR(100) NOT NULL,
    proficiency_level VARCHAR(50)
);

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    role VARCHAR(100),
    start_date DATE,
    end_date DATE,
    project_url TEXT,
    technologies_used JSONB,
    is_featured BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    date DATE,
    issuer VARCHAR(255),
    achievement_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE profile_languages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    language VARCHAR(100) NOT NULL,
    proficiency VARCHAR(50)
);

CREATE TABLE profile_interests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    interest_tag VARCHAR(100) NOT NULL
);

CREATE TABLE profile_social_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    platform VARCHAR(50) NOT NULL,
    url TEXT NOT NULL
);

CREATE TABLE profile_visibility_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    visibility_type VARCHAR(50) NOT NULL,
    field_name VARCHAR(100) NOT NULL
);
