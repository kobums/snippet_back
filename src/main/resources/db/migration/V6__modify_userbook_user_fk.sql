-- V6__modify_userbook_user_fk.sql
-- 1. 기존 데이터 정리 (기존 문자열 기반 userId 데이터를 모두 지우거나, 1번 유저로 매핑)
-- 로컬/개발 환경이므로 데이터 정합성을 위해 일괄 삭제 후 외래키를 맺는 방식을 선택합니다.
-- 필요하다면 'TRUNCATE TABLE userbook_tb;' 를 사용해도 됩니다.
DELETE FROM userbook_tb;

-- 2. 테스트용 1번 유저가 없으면 만듭니다. (임시)
INSERT IGNORE INTO user_tb (u_id, u_email, u_password, u_name, u_createdate) VALUES (1, 'test_user_1@test.com', 'password', 'TestUser', NOW());

-- 3. 기존 UK 삭제 및 컬럼 삭제
ALTER TABLE userbook_tb DROP INDEX uk_userbook_user_book;
ALTER TABLE userbook_tb DROP COLUMN ub_userid;

-- 4. 새로운 ub_user (BIGINT) 컬럼 추가
ALTER TABLE userbook_tb ADD COLUMN ub_user BIGINT NOT NULL COMMENT '사용자 FK (user_tb.u_id)' AFTER ub_id;

-- 5. 외래키 및 새로운 UK 추가
ALTER TABLE userbook_tb ADD CONSTRAINT fk_userbook_user FOREIGN KEY (ub_user) REFERENCES user_tb (u_id) ON DELETE CASCADE;
ALTER TABLE userbook_tb ADD UNIQUE KEY uk_userbook_user_book (ub_user, ub_book);
