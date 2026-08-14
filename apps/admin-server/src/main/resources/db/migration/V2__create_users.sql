CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_users_email_normalized CHECK (email = lower(email)),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_users_failed_login_attempts CHECK (failed_login_attempts >= 0)
);

CREATE UNIQUE INDEX ux_users_email ON users (email);
CREATE INDEX ix_users_status ON users (status);
