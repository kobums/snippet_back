ALTER TABLE user_tb
    ADD COLUMN u_verified TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN u_verification_code VARCHAR(6) NULL,
    ADD COLUMN u_code_expires_at DATETIME NULL;
