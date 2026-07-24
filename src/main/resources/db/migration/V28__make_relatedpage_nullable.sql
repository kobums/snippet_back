-- V28__make_relatedpage_nullable.sql
-- r_relatedpage는 V3에서 NOT NULL DEFAULT 0으로 추가됐지만,
-- 엔티티(Snippet.relatedPage)는 nullable Integer이고 Hibernate가 insert에 컬럼을 항상 포함하므로
-- 페이지 미입력 시 null insert → SQLIntegrityConstraintViolation으로 기록 생성이 실패했다.
-- 페이지는 선택 입력이므로 컬럼을 nullable로 변경한다.
ALTER TABLE record_tb MODIFY COLUMN r_relatedpage INT NULL COMMENT '관련 페이지 번호 (선택)';

-- V3 백필 당시 '페이지 없음'을 의미하던 0 값을 NULL로 정리 (0페이지는 실존하지 않음)
UPDATE record_tb SET r_relatedpage = NULL WHERE r_relatedpage = 0;
