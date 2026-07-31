CREATE TABLE startups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    founder_id UUID NOT NULL,
    startup_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    tagline VARCHAR(255),
    description TEXT,
    industry VARCHAR(100),
    stage VARCHAR(50),
    founded_year INT,
    logo_url TEXT,
    website TEXT,
    tech_stack_json JSONB DEFAULT '[]'::jsonb,
    problem_statement TEXT,
    solution TEXT,
    target_market TEXT,
    business_model TEXT,
    revenue_model TEXT,
    current_mrr NUMERIC(15,2),
    seeking_type VARCHAR(50),
    funding_goal NUMERIC(15,2),
    equity_offered NUMERIC(5,2),
    pitch_deck_url TEXT,
    demo_url TEXT,
    is_incubated BOOLEAN DEFAULT false,
    incubator_name VARCHAR(255),
    is_verified BOOLEAN DEFAULT false,
    follower_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE startup_team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    startup_id UUID NOT NULL,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    role VARCHAR(100),
    equity_pct NUMERIC(5,2),
    is_founder BOOLEAN DEFAULT false,
    is_looking_for_cofounder BOOLEAN DEFAULT false,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cofounder_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    startup_id UUID NOT NULL,
    requester_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    role_needed VARCHAR(100),
    skills_required_json JSONB DEFAULT '[]'::jsonb,
    equity_offered NUMERIC(5,2),
    message TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE investors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    investor_type VARCHAR(50),
    investment_stages_json JSONB DEFAULT '[]'::jsonb,
    sectors_json JSONB DEFAULT '[]'::jsonb,
    min_ticket_size NUMERIC(15,2),
    max_ticket_size NUMERIC(15,2),
    currency VARCHAR(10),
    portfolio_companies_count INT DEFAULT 0,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE funding_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    startup_id UUID NOT NULL,
    investor_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    amount_requested NUMERIC(15,2),
    currency VARCHAR(10),
    equity_offered NUMERIC(5,2),
    status VARCHAR(50) DEFAULT 'PENDING',
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE businesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    business_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(100),
    description TEXT,
    logo_url TEXT,
    cover_url TEXT,
    website TEXT,
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    country VARCHAR(100),
    operating_hours_json JSONB DEFAULT '{}'::jsonb,
    is_verified BOOLEAN DEFAULT false,
    average_rating NUMERIC(3,2) DEFAULT 0,
    review_count INT DEFAULT 0,
    is_accepting_appointments BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE business_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2),
    currency VARCHAR(10),
    duration_minutes INT,
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE business_products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10,2),
    currency VARCHAR(10),
    stock_qty INT,
    images_json JSONB DEFAULT '[]'::jsonb,
    is_active BOOLEAN DEFAULT true
);

CREATE TABLE business_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    reviewer_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    rating NUMERIC(3,2) NOT NULL,
    review_text TEXT,
    images_json JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE business_appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL,
    service_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INT,
    status VARCHAR(50) DEFAULT 'PENDING',
    notes TEXT,
    amount NUMERIC(10,2),
    payment_status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE collaboration_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    requester_id UUID NOT NULL,
    type VARCHAR(100),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    budget NUMERIC(15,2),
    timeline VARCHAR(100),
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
