ALTER TABLE users ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE password_reset_tokens ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE password_reset_tokens ADD CONSTRAINT ck_password_reset_failed_attempts CHECK (failed_attempts BETWEEN 0 AND 5);
CREATE INDEX idx_password_reset_active_user ON password_reset_tokens(user_id, created_at DESC) WHERE used_at IS NULL;
