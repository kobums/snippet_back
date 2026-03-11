-- V8__update_userbook_type_status.sql
-- type/status 체계 개선

-- 1. ub_type 컬럼 코멘트 수정
--    wish: 위시리스트, have: 소장, borrow: 대여 중, return: 반납 완료
ALTER TABLE userbook_tb
  MODIFY COLUMN ub_type VARCHAR(20) NOT NULL
    COMMENT '도서 분류 타입 (wish: 위시리스트, have: 소장, borrow: 대여 중, return: 반납 완료)';

-- 2. ub_status 컬럼 코멘트 수정
--    none: 위시리스트 전용 (type=wish일 때만 사용)
--    waiting: 읽기 대기 중 (have or borrow)
--    reading: 읽는 중 (have or borrow)
--    completed: 완독
--    dropped: 독서 중단
ALTER TABLE userbook_tb
  MODIFY COLUMN ub_status VARCHAR(20) NOT NULL
    COMMENT '독서 상태 (none: 위시 전용, waiting: 대기 중, reading: 읽는 중, completed: 완독, dropped: 중단)';

-- 3. 기존 type=wish 데이터의 status를 none으로 업데이트
UPDATE userbook_tb
SET ub_status = 'none'
WHERE ub_type = 'wish';
