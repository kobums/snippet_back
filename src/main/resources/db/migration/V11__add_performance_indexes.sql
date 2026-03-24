-- V11: 성능 최적화를 위한 복합 인덱스 추가
-- 날짜 범위 쿼리 및 자주 사용되는 조회 패턴에 대한 인덱스

-- 1. UserBook 날짜 범위 조회 최적화
-- getUserBooksByMonth에서 사용 (ub_user, ub_status, ub_startdate, ub_enddate 조합)
CREATE INDEX idx_userbook_user_status_dates
ON userbook_tb(ub_user, ub_status, ub_startdate, ub_enddate);

-- 2. Record(Snippet) 사용자별 타입/날짜 조회 최적화
-- findByUserAndTypeAndCreateDateBetween 등에서 사용
CREATE INDEX idx_record_user_type_createdate
ON record_tb(r_user, r_type, r_createdate DESC);

-- 3. SnippetArchive 사용자별 조회 최적화
-- findByUserOrderByCreateDateDesc에서 사용
CREATE INDEX idx_usersnippet_user_createdate
ON usersnippet_tb(us_user, us_createdate DESC);

-- 4. UserBook 통계 쿼리 최적화 (endDate 기준 월별/연별 집계)
-- getMonthlyStats, getYearlyStats에서 사용
CREATE INDEX idx_userbook_user_status_enddate
ON userbook_tb(ub_user, ub_status, ub_enddate);
