-- 기존 대출 도서 중 반납 예정일이 비어 있는 행을 대출 날짜(생성일) + 2주로 백필
-- (신규 대출은 애플리케이션에서 생성 시점에 +2주 자동 설정됨)
UPDATE userbook_tb
SET ub_returndate = DATE_ADD(ub_createdate, INTERVAL 14 DAY)
WHERE ub_type = 'borrow'
  AND ub_returndate IS NULL;
