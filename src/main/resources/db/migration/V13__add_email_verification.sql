ALTER TABLE user_tb
    ADD COLUMN u_verified TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN u_verification_code VARCHAR(6) NULL,
    ADD COLUMN u_code_expires_at DATETIME NULL;

-- 기존 사용자는 인증된 것으로 처리
UPDATE user_tb SET u_verified = 1;
