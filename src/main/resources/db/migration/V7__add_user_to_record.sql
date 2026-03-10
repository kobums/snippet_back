-- V7__add_user_to_record.sql
-- record_tb에 사용자 FK 추가 (기록의 소유자)

-- 1. r_user 컬럼 추가
ALTER TABLE record_tb ADD COLUMN r_user BIGINT NOT NULL DEFAULT 1 COMMENT '사용자 FK (user_tb.u_id)' AFTER r_book;

-- 2. 기본값 제거 (이후 INSERT 시 반드시 명시하도록)
ALTER TABLE record_tb ALTER COLUMN r_user DROP DEFAULT;

-- 3. 외래키 및 인덱스 추가
ALTER TABLE record_tb ADD CONSTRAINT fk_record_user FOREIGN KEY (r_user) REFERENCES user_tb (u_id) ON DELETE CASCADE;
CREATE INDEX idx_record_user ON record_tb (r_user);
CREATE INDEX idx_record_user_type ON record_tb (r_user, r_type);
