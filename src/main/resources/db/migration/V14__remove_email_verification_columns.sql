ALTER TABLE user_tb
    DROP COLUMN IF EXISTS u_verified,
    DROP COLUMN IF EXISTS u_verification_code,
    DROP COLUMN IF EXISTS u_code_expires_at;
