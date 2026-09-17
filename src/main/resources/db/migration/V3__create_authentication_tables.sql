CREATE UNIQUE INDEX uq_users_email
    ON users (email)
    WHERE email IS NOT NULL;

CREATE UNIQUE INDEX uq_users_student_number
    ON users (student_number)
    WHERE student_number IS NOT NULL;

CREATE TABLE oauth_accounts (
    oauth_account_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_oauth_accounts_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_oauth_accounts_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts (user_id);

CREATE TABLE auth_sessions (
    auth_session_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    session_key UUID NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    device_info VARCHAR(500) NULL,
    ip_address VARCHAR(45) NULL,
    last_used_at TIMESTAMPTZ NULL,
    revoked_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT uq_auth_sessions_session_key UNIQUE (session_key),
    CONSTRAINT uq_auth_sessions_refresh_hash UNIQUE (refresh_token_hash)
);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX idx_auth_sessions_expires_at ON auth_sessions (expires_at);
