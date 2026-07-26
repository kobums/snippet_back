-- V31(출판일)과 같은 불일치 정리: Book 엔티티가 nullable로 선언한 나머지 컬럼.
-- V3가 NOT NULL DEFAULT로 만들었지만, INSERT가 명시적으로 NULL을 넣으면 DEFAULT가
-- 적용되지 않아 출판사/총페이지 없는 도서 등록이 500으로 실패했다.
ALTER TABLE book_tb MODIFY COLUMN b_publisher VARCHAR(100) NULL COMMENT '출판사';
ALTER TABLE book_tb MODIFY COLUMN b_totalpage INT NULL COMMENT '총 페이지 수';
