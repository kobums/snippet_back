-- V3__reading_system_schema.sql

-- 1. book_tb 테이블 컬럼 추가
ALTER TABLE book_tb ADD COLUMN b_publisher VARCHAR(100);
ALTER TABLE book_tb ADD COLUMN b_totalpage INT;
ALTER TABLE book_tb ADD COLUMN b_publicationdate DATE;

-- 2. userbook_tb 테이블 생성
CREATE TABLE userbook_tb (
  ub_id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '서재 아이템 고유 ID',
  ub_userid     VARCHAR(100) NOT NULL                COMMENT '사용자 식별자 (임시/로컬스토리지용)',
  ub_book       BIGINT       NOT NULL                COMMENT '책 FK (book_tb.b_id)',
  ub_status     VARCHAR(20)  NOT NULL DEFAULT 'wish' COMMENT '상태 (wish, waiting, reading, completed, dropped)',
  ub_readpage   INT          NOT NULL DEFAULT 0      COMMENT '읽은 페이지 수 (진도율)',
  ub_createdate DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
  ub_updatedate DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일',

  PRIMARY KEY (ub_id),
  CONSTRAINT fk_userbook_book FOREIGN KEY (ub_book) REFERENCES book_tb (b_id),
  UNIQUE KEY uk_userbook_user_book (ub_userid, ub_book)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='이용자별 도서 상태(서재)';

-- 3. record_tb (다차원 기록 테이블) 설계
-- 기존 snippet_tb를 확장하는 개념이므로, 테이블 이름을 변경하고 컬럼을 추가/수정합니다.
ALTER TABLE snippet_tb RENAME TO record_tb;

-- 컬럼명 및 타입 변경 (s_ 접두사를 r_ 로 변경)
ALTER TABLE record_tb CHANGE COLUMN s_id r_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE record_tb CHANGE COLUMN s_book r_book BIGINT NOT NULL COMMENT '책 FK (book_tb.b_id)';
ALTER TABLE record_tb CHANGE COLUMN s_text r_content TEXT NOT NULL COMMENT '문장 또는 일기/리뷰 내용';
ALTER TABLE record_tb CHANGE COLUMN s_tag r_tag VARCHAR(50) COMMENT '카테고리 태그';
ALTER TABLE record_tb CHANGE COLUMN s_createdate r_createdate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일';

-- 신규 컬럼 추가
ALTER TABLE record_tb ADD COLUMN r_type VARCHAR(20) NOT NULL DEFAULT 'snippet' COMMENT '기록 타입 (snippet, diary, review)';
ALTER TABLE record_tb ADD COLUMN r_relatedpage INT COMMENT '관련 페이지 번호';

-- 인덱스 이름 변경
-- MariaDB 10.5.2 이전 버전 고려하여 기존 인덱스 DROP 후 생성
DROP INDEX idx_snippet_book ON record_tb;
DROP INDEX idx_snippet_tag ON record_tb;
CREATE INDEX idx_record_book ON record_tb (r_book);
CREATE INDEX idx_record_tag  ON record_tb (r_tag);
CREATE INDEX idx_record_type ON record_tb (r_type);
