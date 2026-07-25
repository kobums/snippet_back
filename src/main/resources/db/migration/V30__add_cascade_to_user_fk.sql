-- 회원 탈퇴(DELETE /api/auth/account)가 FK 제약에 막혀 항상 실패하던 문제 수정.
--
-- user_tb를 참조하는 FK 중 아래 3개에 ON DELETE 절이 없어 기본값 RESTRICT로 동작했다.
-- 스니펫을 한 번이라도 보관하거나(usersnippet_tb), 카드를 열람하거나(snippet_daily_view_tb),
-- 독서 목표를 설정한(readinggoal_tb) 사용자는 사실상 전부 삭제가 차단됐다.
--
-- 추가로 fk_us_snippet(record_tb 참조)도 RESTRICT였다. user_tb 삭제가 record_tb로
-- 연쇄되는데, 그 스니펫을 다른 사용자가 보관 중이면 2차로 다시 막힌다
-- (스니펫 카드는 작성자 구분 없이 배포되므로 충분히 발생 가능).

-- 스니펫 보관함 — 탈퇴자 본인의 보관 기록 삭제
ALTER TABLE usersnippet_tb DROP FOREIGN KEY IF EXISTS fk_us_user;
ALTER TABLE usersnippet_tb
    ADD CONSTRAINT fk_us_user FOREIGN KEY (us_user) REFERENCES user_tb (u_id) ON DELETE CASCADE;

-- 스니펫 보관함 — 원본 스니펫이 사라지면 다른 사용자의 보관 기록도 함께 정리
ALTER TABLE usersnippet_tb DROP FOREIGN KEY IF EXISTS fk_us_snippet;
ALTER TABLE usersnippet_tb
    ADD CONSTRAINT fk_us_snippet FOREIGN KEY (us_snippet) REFERENCES record_tb (r_id) ON DELETE CASCADE;

-- 일일 카드 열람 카운트
ALTER TABLE snippet_daily_view_tb DROP FOREIGN KEY IF EXISTS fk_sdv_user;
ALTER TABLE snippet_daily_view_tb
    ADD CONSTRAINT fk_sdv_user FOREIGN KEY (sdv_user) REFERENCES user_tb (u_id) ON DELETE CASCADE;

-- 독서 목표
ALTER TABLE readinggoal_tb DROP FOREIGN KEY IF EXISTS fk_readinggoal_user;
ALTER TABLE readinggoal_tb
    ADD CONSTRAINT fk_readinggoal_user FOREIGN KEY (rg_userid) REFERENCES user_tb (u_id) ON DELETE CASCADE;
