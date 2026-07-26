-- 출판일을 nullable로 변경.
-- Book 엔티티와 BookController는 publicationDate 미전달을 허용(null 저장)하는데
-- V3에서 NOT NULL로 생성돼 있어, 출판일 없는 도서 등록 시 500이 발생했다.
-- (NOT NULL DEFAULT CURRENT_DATE는 INSERT가 명시적으로 NULL을 넣으면 적용되지 않는다)
ALTER TABLE book_tb MODIFY COLUMN b_publicationdate DATE NULL COMMENT '출판일';
