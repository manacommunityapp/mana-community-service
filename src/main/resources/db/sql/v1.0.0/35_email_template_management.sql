-- FILE: db/sql/v1.0.0/35_email_template_management.sql
-- Multi-tenant email template management foundation.
-- Required for prod (ddl-auto: validate); local/dev profiles can auto-create via Hibernate.

CREATE TABLE IF NOT EXISTS email_category (
    id              BIGSERIAL       PRIMARY KEY,
    code            VARCHAR(80)     NOT NULL UNIQUE,
    name            VARCHAR(140)    NOT NULL,
    description     VARCHAR(500),
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS community_branding (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL UNIQUE REFERENCES community(id),
    logo_url        VARCHAR(500),
    primary_color   VARCHAR(20)     NOT NULL DEFAULT '#2563eb',
    secondary_color VARCHAR(20)     DEFAULT '#0f172a',
    font_family     VARCHAR(120),
    button_style    VARCHAR(40),
    banner_url      VARCHAR(500),
    facebook_url    VARCHAR(500),
    instagram_url   VARCHAR(500),
    website_url     VARCHAR(500),
    support_email   VARCHAR(160),
    support_phone   VARCHAR(40),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_header (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    name            VARCHAR(120)    NOT NULL,
    html_content    TEXT            NOT NULL,
    layout_json     TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    version         INTEGER         NOT NULL DEFAULT 1,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_footer (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    name            VARCHAR(120)    NOT NULL,
    html_content    TEXT            NOT NULL,
    layout_json     TEXT,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    version         INTEGER         NOT NULL DEFAULT 1,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_template (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    template_code   VARCHAR(80)     NOT NULL,
    template_name   VARCHAR(160)    NOT NULL,
    category        VARCHAR(80),
    subject         VARCHAR(240)    NOT NULL,
    html_content    TEXT            NOT NULL,
    layout_json     TEXT,
    header_id       BIGINT          REFERENCES email_header(id),
    footer_id       BIGINT          REFERENCES email_footer(id),
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    version         INTEGER         NOT NULL DEFAULT 1,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_template_community_code UNIQUE (community_id, template_code),
    CONSTRAINT ck_email_template_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE IF NOT EXISTS email_template_version (
    id              BIGSERIAL       PRIMARY KEY,
    template_id     BIGINT          NOT NULL REFERENCES email_template(id) ON DELETE CASCADE,
    version         INTEGER         NOT NULL,
    subject         VARCHAR(240)    NOT NULL,
    html_content    TEXT            NOT NULL,
    layout_json     TEXT,
    header_id       BIGINT          REFERENCES email_header(id),
    footer_id       BIGINT          REFERENCES email_footer(id),
    status          VARCHAR(20)     NOT NULL,
    created_by      BIGINT,
    notes           VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_template_version UNIQUE (template_id, version),
    CONSTRAINT ck_email_template_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE IF NOT EXISTS email_component (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          REFERENCES community(id),
    component_type  VARCHAR(80)     NOT NULL,
    name            VARCHAR(140)    NOT NULL,
    html_content    TEXT            NOT NULL,
    layout_json     TEXT,
    thumbnail_url   VARCHAR(500),
    is_system       BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_image (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    file_name       VARCHAR(180)    NOT NULL,
    image_url       VARCHAR(500)    NOT NULL,
    alt_text        VARCHAR(240),
    image_type      VARCHAR(60),
    uploaded_by     BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_dynamic_field (
    id              BIGSERIAL       PRIMARY KEY,
    category        VARCHAR(80)     NOT NULL,
    field_key       VARCHAR(120)    NOT NULL UNIQUE,
    display_name    VARCHAR(140)    NOT NULL,
    sample_value    VARCHAR(500),
    description     VARCHAR(500),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order      INTEGER         NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS email_preview_data (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          NOT NULL REFERENCES community(id),
    template_code   VARCHAR(80)     NOT NULL,
    name            VARCHAR(140)    NOT NULL,
    sample_json     TEXT            NOT NULL,
    created_by      BIGINT,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS email_audit (
    id              BIGSERIAL       PRIMARY KEY,
    community_id    BIGINT          REFERENCES community(id),
    template_id     BIGINT          REFERENCES email_template(id),
    action          VARCHAR(60)     NOT NULL,
    actor_id        BIGINT,
    details         VARCHAR(1000),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_header_community ON email_header(community_id);
CREATE INDEX IF NOT EXISTS idx_email_footer_community ON email_footer(community_id);
CREATE INDEX IF NOT EXISTS idx_email_template_community_status ON email_template(community_id, status);
CREATE INDEX IF NOT EXISTS idx_email_template_code ON email_template(template_code);
CREATE INDEX IF NOT EXISTS idx_email_component_community_type ON email_component(community_id, component_type);
CREATE INDEX IF NOT EXISTS idx_email_image_community ON email_image(community_id);
CREATE INDEX IF NOT EXISTS idx_email_preview_community_code ON email_preview_data(community_id, template_code);
CREATE INDEX IF NOT EXISTS idx_email_audit_template ON email_audit(template_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_header_community_name ON email_header(community_id, name);
CREATE UNIQUE INDEX IF NOT EXISTS uk_email_footer_community_name ON email_footer(community_id, name);

INSERT INTO email_header (community_id, name, html_content, layout_json, is_active, version)
SELECT
    c.id,
    'Default Header',
    '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#2563eb;color:#ffffff;font-family:Arial,sans-serif;"><tr><td style="padding:20px 28px;font-size:20px;font-weight:700;">{{communityName}}</td></tr></table>',
    '{"type":"header","blocks":[{"type":"brand"}]}',
    TRUE,
    1
FROM community c
ON CONFLICT (community_id, name) DO NOTHING;

INSERT INTO email_footer (community_id, name, html_content, layout_json, is_active, version)
SELECT
    c.id,
    'Default Footer',
    '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8fafc;color:#64748b;font-family:Arial,sans-serif;"><tr><td style="padding:18px 28px;font-size:12px;">&copy; {{year}} {{communityName}}. This is an automated message.</td></tr></table>',
    '{"type":"footer","blocks":[{"type":"legal"}]}',
    TRUE,
    1
FROM community c
ON CONFLICT (community_id, name) DO NOTHING;

INSERT INTO email_template (
    community_id,
    template_code,
    template_name,
    category,
    subject,
    html_content,
    layout_json,
    header_id,
    footer_id,
    status,
    version
)
SELECT
    c.id,
    seed.template_code,
    seed.template_name,
    seed.category,
    seed.subject,
    seed.html_content,
    seed.layout_json,
    (SELECT h.id FROM email_header h WHERE h.community_id = c.id AND h.name = 'Default Header' ORDER BY h.id LIMIT 1),
    (SELECT f.id FROM email_footer f WHERE f.community_id = c.id AND f.name = 'Default Footer' ORDER BY f.id LIMIT 1),
    'ACTIVE',
    1
FROM community c
CROSS JOIN (
    VALUES
        (
            'EMAIL_OTP',
            'Email Verification OTP',
            'AUTH',
            'Your verification code',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#111827;font-size:22px;">Verify your email</h1><p style="color:#374151;font-size:14px;line-height:1.6;">Hi {{recipientName}}, use this code to verify your email address.</p><div style="margin:24px 0;padding:18px;background:#eff6ff;border-radius:8px;text-align:center;font-size:30px;font-weight:700;letter-spacing:6px;color:#1d4ed8;">{{otpCode}}</div><p style="color:#64748b;font-size:13px;">This code expires in {{expiryMinutes}} minutes.</p></td></tr></table>',
            '{"sections":[{"type":"text"},{"type":"otp"}]}'
        ),
        (
            'REGISTRATION_RECEIVED',
            'Registration Received',
            'SPORTS',
            'We received your registration',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#111827;font-size:22px;">Registration received</h1><p>Hi {{recipientName}}, we received your registration for <strong>{{eventName}}</strong>.</p><p><strong>Sport:</strong> {{sportName}}<br><strong>Category:</strong> {{categoryName}}<br><strong>Format:</strong> {{matchType}}<br><strong>Dates:</strong> {{eventDates}}<br><strong>Venue:</strong> {{venueName}}</p><p>Status: <strong>{{status}}</strong></p><p><a href="{{actionUrl}}" style="background:#2563eb;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View registration</a></p></td></tr></table>',
            '{"sections":[{"type":"event-card"},{"type":"button"}]}'
        ),
        (
            'REGISTRATION_CONFIRMED',
            'Registration Confirmed',
            'SPORTS',
            'You are confirmed!',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#065f46;font-size:22px;">You are confirmed</h1><p>Great news, {{recipientName}}. Your registration for <strong>{{eventName}}</strong> has been confirmed.</p><p><strong>Tournament:</strong> {{tournamentName}}<br><strong>Sport:</strong> {{sportName}}<br><strong>Category:</strong> {{categoryName}}<br><strong>Dates:</strong> {{eventDates}}<br><strong>Venue:</strong> {{venueName}}<br><strong>Confirmed at:</strong> {{confirmedAt}}</p><p><a href="{{actionUrl}}" style="background:#059669;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">Go to dashboard</a></p></td></tr></table>',
            '{"sections":[{"type":"status"},{"type":"event-card"},{"type":"button"}]}'
        ),
        (
            'REGISTRATION_REJECTED',
            'Registration Rejected',
            'SPORTS',
            'Update on your registration',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#334155;font-size:22px;">Registration update</h1><p>Hi {{recipientName}}, thank you for your interest in <strong>{{eventName}}</strong>. Your registration could not be approved.</p><p><strong>Sport:</strong> {{sportName}}<br><strong>Category:</strong> {{categoryName}}<br><strong>Dates:</strong> {{eventDates}}<br><strong>Venue:</strong> {{venueName}}</p><p style="background:#fef2f2;border:1px solid #fecaca;padding:12px;border-radius:6px;">{{reason}}</p><p><a href="{{actionUrl}}" style="background:#64748b;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">Browse other events</a></p></td></tr></table>',
            '{"sections":[{"type":"status"},{"type":"event-card"},{"type":"button"}]}'
        ),
        (
            'REGISTRATION_OPEN',
            'Registration Open',
            'SPORTS',
            'Registrations are now open!',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#111827;font-size:24px;">Registrations are open</h1><p>Hi {{recipientName}}, registrations are open for <strong>{{tournamentName}}</strong>.</p><p>{{description}}</p><p><strong>Sports:</strong> {{sportList}}<br><strong>Registration:</strong> {{registrationPeriod}}<br><strong>Event dates:</strong> {{eventDates}}<br><strong>Venue:</strong> {{venueName}}<br><strong>Organizer:</strong> {{contactDetails}}</p><p><a href="{{actionUrl}}" style="background:#4f46e5;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">Register now</a></p></td></tr></table>',
            '{"sections":[{"type":"hero"},{"type":"details"},{"type":"button"}]}'
        ),
        (
            'SCHEDULE_PUBLISHED',
            'Schedule Published',
            'SPORTS',
            'Match schedule is live',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#111827;font-size:22px;">Schedule published</h1><p>Hi {{recipientName}}, the schedule for <strong>{{tournamentName}}</strong> is live.</p><p><strong>Event:</strong> {{eventName}}<br><strong>Sport:</strong> {{sportName}}<br><strong>Starts:</strong> {{startDate}}<br><strong>Venue:</strong> {{venueName}}<br><strong>Matches:</strong> {{matchCount}}</p><p><a href="{{actionUrl}}" style="background:#7c3aed;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View schedule</a></p></td></tr></table>',
            '{"sections":[{"type":"details"},{"type":"button"}]}'
        ),
        (
            'MATCH_REMINDER',
            'Match Reminder',
            'SPORTS',
            'Your match starts soon',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#991b1b;font-size:22px;">Match reminder</h1><p>Hi {{recipientName}}, your {{roundName}} match in <strong>{{tournamentName}}</strong> starts in about {{minutesUntilStart}} minutes.</p><p><strong>{{homeTeam}}</strong> vs <strong>{{awayTeam}}</strong></p><p><strong>Date:</strong> {{matchDate}}<br><strong>Time:</strong> {{matchTime}}<br><strong>Venue:</strong> {{venueName}}<br><strong>Court:</strong> {{courtName}}</p><p><a href="{{actionUrl}}" style="background:#dc2626;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View match</a></p></td></tr></table>',
            '{"sections":[{"type":"match-card"},{"type":"button"}]}'
        ),
        (
            'WINNER_NOTIFICATION',
            'Winner Notification',
            'SPORTS',
            'You won your match!',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#065f46;font-size:22px;">You won!</h1><p>Congratulations {{recipientName}}. You won your {{roundName}} match in <strong>{{tournamentName}}</strong> against {{opponentName}}.</p><p><strong>Score:</strong> {{score}}</p><p>{{nextRoundInfo}}</p><p><a href="{{actionUrl}}" style="background:#059669;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View next fixture</a></p></td></tr></table>',
            '{"sections":[{"type":"winner-card"},{"type":"button"}]}'
        ),
        (
            'TOURNAMENT_COMPLETION',
            'Tournament Completion',
            'SPORTS',
            'Tournament results are in',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#92400e;font-size:22px;">Tournament results</h1><p>Hi {{recipientName}}, <strong>{{tournamentName}}</strong> has finished.</p><p><strong>Champion:</strong> {{championName}}<br><strong>Runner-up:</strong> {{runnerUpName}}<br><strong>Third place:</strong> {{thirdPlaceName}}</p><p><a href="{{actionUrl}}" style="background:#b45309;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View full results</a></p></td></tr></table>',
            '{"sections":[{"type":"results"},{"type":"button"}]}'
        ),
        (
            'PRIZE_DISTRIBUTION',
            'Prize Distribution',
            'SPORTS',
            'Prize distribution details',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#4c1d95;font-size:22px;">Prize distribution</h1><p>Congratulations {{recipientName}}. You are the <strong>{{position}}</strong> of <strong>{{tournamentName}}</strong>.</p><p><strong>Prize:</strong> {{prize}}<br><strong>Ceremony:</strong> {{ceremonyDate}}<br><strong>Venue:</strong> {{venueName}}</p><p><a href="{{actionUrl}}" style="background:#7c3aed;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View details</a></p></td></tr></table>',
            '{"sections":[{"type":"prize-card"},{"type":"button"}]}'
        ),
        (
            'TOURNAMENT_OPEN',
            'Tournament Open',
            'SPORTS',
            'Registration is now open!',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#065f46;font-size:24px;">{{tournamentName}} is open</h1><p>Hi {{recipientName}}, {{customMessage}}</p><p><strong>Event date:</strong> {{eventDate}}<br><strong>Register by:</strong> {{registrationDeadline}}<br><strong>Sports:</strong> {{sportList}}<br><strong>Venue:</strong> {{venueName}}<br><strong>Contact:</strong> {{contactName}} {{contactNumber}}</p><p><a href="{{actionUrl}}" style="background:#059669;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">Register now</a></p></td></tr></table>',
            '{"sections":[{"type":"hero"},{"type":"details"},{"type":"button"}]}'
        ),
        (
            'TOURNAMENT_ANNOUNCEMENT',
            'Tournament Announcement',
            'SPORTS',
            'Tournament announcement',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 8px;color:#111827;font-size:24px;">{{tournamentName}}</h1><p style="color:#475569;">{{tournamentDescription}}</p><p style="background:#fff7ed;border-left:4px solid #f59e0b;padding:12px;border-radius:6px;">{{customMessage}}</p><p><strong>Events:</strong> {{totalEvents}}<br><strong>Sports:</strong> {{totalSports}}<br><strong>Participants:</strong> {{expectedParticipants}}+<br><strong>Starts:</strong> {{eventStartDate}}<br><strong>Venue:</strong> {{venueName}}</p><p><a href="{{actionUrl}}" style="background:#2563eb;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View tournament</a></p><p style="font-size:13px;color:#64748b;">Need help? {{supportEmail}} {{supportPhone}}</p></td></tr></table>',
            '{"sections":[{"type":"hero"},{"type":"announcement"},{"type":"stats"},{"type":"button"}]}'
        ),
        (
            'TOURNAMENT_START',
            'Tournament Start',
            'SPORTS',
            'Welcome to the tournament!',
            '<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="font-family:Arial,sans-serif;background:#ffffff;"><tr><td style="padding:28px;"><h1 style="margin:0 0 12px;color:#1d4ed8;font-size:24px;">Tournament started</h1><p>Hi {{recipientName}}, welcome to <strong>{{tournamentName}}</strong>.</p><p>{{description}}</p><p><strong>Sports:</strong> {{sportName}}<br><strong>Opening time:</strong> {{openingTime}}<br><strong>Venue:</strong> {{venueName}}<br><strong>Opening match:</strong> {{firstFixture}}</p><p><a href="{{actionUrl}}" style="background:#2563eb;color:#ffffff;padding:12px 18px;border-radius:6px;text-decoration:none;">View live fixtures</a></p></td></tr></table>',
            '{"sections":[{"type":"hero"},{"type":"details"},{"type":"button"}]}'
        )
) AS seed(template_code, template_name, category, subject, html_content, layout_json)
ON CONFLICT (community_id, template_code) DO NOTHING;

INSERT INTO email_template_version (
    template_id,
    version,
    subject,
    html_content,
    layout_json,
    header_id,
    footer_id,
    status,
    notes
)
SELECT
    t.id,
    t.version,
    t.subject,
    t.html_content,
    t.layout_json,
    t.header_id,
    t.footer_id,
    t.status,
    'Initial database-backed sample template'
FROM email_template t
WHERE NOT EXISTS (
    SELECT 1
    FROM email_template_version v
    WHERE v.template_id = t.id
      AND v.version = t.version
);

INSERT INTO email_dynamic_field (category, field_key, display_name, sample_value, description, sort_order)
VALUES
    ('Community', 'communityName', 'Community Name', 'Green Valley Apartments', 'Current community name.', 10),
    ('Community', 'logoUrl', 'Logo URL', 'https://cdn.example.com/logo.png', 'Community logo from branding.', 20),
    ('Community', 'primaryColor', 'Primary Color', '#2563eb', 'Community primary brand color.', 30),
    ('Community', 'supportEmail', 'Support Email', 'support@manacommunity.app', 'Community support email.', 40),
    ('Tournament', 'tournamentName', 'Tournament Name', 'Summer Sports League', 'Tournament display name.', 100),
    ('Tournament', 'venue', 'Venue', 'Main Clubhouse Ground', 'Tournament or match venue.', 110),
    ('Tournament', 'date', 'Date', '25 July 2026', 'Tournament date.', 120),
    ('Tournament', 'startTime', 'Start Time', '9:00 AM', 'Tournament start time.', 130),
    ('Tournament', 'endTime', 'End Time', '6:00 PM', 'Tournament end time.', 140),
    ('Sports', 'sportList', 'Sport List', 'Cricket, Badminton, Chess', 'Comma separated sport list.', 200),
    ('Sports', 'winners', 'Winners', 'Team A', 'Winner list or summary.', 210),
    ('Payments', 'amount', 'Amount', '1500', 'Payment or invoice amount.', 300),
    ('Payments', 'invoiceNumber', 'Invoice Number', 'INV-2026-001', 'Invoice reference.', 310),
    ('User', 'firstName', 'First Name', 'Aarav', 'Recipient first name.', 400),
    ('User', 'lastName', 'Last Name', 'Sharma', 'Recipient last name.', 410)
ON CONFLICT (field_key) DO NOTHING;
