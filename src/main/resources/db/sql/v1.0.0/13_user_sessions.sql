-- FILE: db/sql/v1.0.0/13_user_sessions.sql
-- Login/session security trail (UserSession entity / SessionService).
-- Required for the prod profile (ddl-auto: validate). local=create / dev=update auto-create it.

CREATE TABLE IF NOT EXISTS user_sessions (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL,
    session_id       VARCHAR(64)  NOT NULL,
    ip_address       VARCHAR(64),
    user_agent       VARCHAR(400),
    device           VARCHAR(40),
    browser          VARCHAR(40),
    login_at         TIMESTAMP    NOT NULL,
    last_activity_at TIMESTAMP,
    logout_at        TIMESTAMP,
    status           VARCHAR(20)  NOT NULL,
    correlation_id   VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_session_user   ON user_sessions (user_id, status);
CREATE INDEX IF NOT EXISTS idx_session_login  ON user_sessions (login_at);
CREATE INDEX IF NOT EXISTS idx_session_status ON user_sessions (status);
