-- V5__add_datetime_userbook.sql
-- (이전에 실패했을 수 있으므로 에러 방지 처리 추가)

-- 1. userbook_tb 테이블 ub_type 컬럼 추가 (존재하면 에러가 날 수 있으나, DROP COLMUN IF EXISTS는 MariaDB 10.8에서 아직 실험적이므로 안전한 방법 사용)
-- ub_status를 VARCHAR(20)으로 통일하고, type(소유상태) 역할을 별도 컬럼으로 분리합니다.

-- 이미 ub_type이 생겼을 수 있으므로 주석 처리 또는 무시 방식을 써야 하지만,
-- SQL 스크립트 상에서 IF NOT EXISTS 를 ALTER ADD에 쓰지 못하므로, 프로시저로 처리합니다.
DELIMITER //
CREATE PROCEDURE AddColumnIfNotExists()
BEGIN
    DECLARE continue handler for 1060 BEGIN END;
    ALTER TABLE userbook_tb ADD COLUMN ub_type VARCHAR(20) NOT NULL DEFAULT 'wish' COMMENT '소유 상태 (wish, borrow, have)';
END //
DELIMITER ;
CALL AddColumnIfNotExists();
DROP PROCEDURE AddColumnIfNotExists;

ALTER TABLE userbook_tb MODIFY COLUMN ub_status VARCHAR(20) NOT NULL DEFAULT 'waiting' COMMENT '읽기 상태 (waiting, reading, completed, dropped)';

-- 2. ub_startdate, ub_enddate 타입을 DATE에서 DATETIME으로 변경
ALTER TABLE userbook_tb MODIFY COLUMN ub_startdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '읽기 시작일';
ALTER TABLE userbook_tb MODIFY COLUMN ub_enddate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '읽기 종료일';
